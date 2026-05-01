package com.summitcraft.summitraffle.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses color-rich message strings into Adventure {@link Component}s.
 *
 * <h2>Supported syntax</h2>
 *
 * <h3>Legacy codes</h3>
 * <pre>
 *   &amp;6Gold text   &amp;lBold   &amp;r Reset
 * </pre>
 *
 * <h3>Hex colour</h3>
 * <pre>
 *   &amp;#FF5733This is orange   or   &amp;#ff5733 also works
 * </pre>
 *
 * <h3>Gradient</h3>
 * <pre>
 *   &lt;gradient:#FF0000:#0000FF&gt;Red to Blue text&lt;/gradient&gt;
 * </pre>
 * Each character in the gradient body receives an interpolated colour.
 * Multiple stops are supported:
 * <pre>
 *   &lt;gradient:#FF0000:#FFFF00:#00FF00&gt;Rainbow&lt;/gradient&gt;
 * </pre>
 *
 * <h3>Mixing</h3>
 * All three syntaxes can appear in the same string:
 * <pre>
 *   &amp;lBold &amp;#FF5733hex &lt;gradient:#FFF:#000&gt;fading&lt;/gradient&gt; &amp;rnormal
 * </pre>
 */
public final class ColorParser {

    private ColorParser() {}

    // ── Regex patterns ────────────────────────────────────────────────────────

    /** Matches &#RRGGBB (case-insensitive, with or without §-prefix after translation) */
    private static final Pattern HEX_PATTERN =
            Pattern.compile("(?i)&(#[0-9A-F]{6})");

    /** Matches <gradient:#HEX1:#HEX2:...>text</gradient> */
    private static final Pattern GRADIENT_PATTERN =
            Pattern.compile("(?i)<gradient:((?:#[0-9A-F]{6}:?)+)>(.*?)</gradient>",
                    Pattern.DOTALL);

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parses a raw config string (with {@code &} codes, {@code &#RRGGBB} hex,
     * and {@code <gradient:...>} tags) into an Adventure Component.
     *
     * @param raw the raw string from config — {@code &} codes not yet translated
     * @return a fully styled Component
     */
    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();

        // Split around gradient blocks first; each segment is processed separately
        List<Component> parts = new ArrayList<>();
        Matcher gm = GRADIENT_PATTERN.matcher(raw);
        int last = 0;

        while (gm.find()) {
            // Text before this gradient block
            if (gm.start() > last) {
                parts.add(parsePlain(raw.substring(last, gm.start())));
            }
            // The gradient block itself
            String stopsStr = gm.group(1);
            String body     = gm.group(2);
            parts.add(buildGradient(stopsStr, body));
            last = gm.end();
        }

        // Remaining text after the last gradient block (or the whole string if none)
        if (last < raw.length()) {
            parts.add(parsePlain(raw.substring(last)));
        }

        if (parts.isEmpty()) return Component.empty();
        TextComponent.Builder builder = Component.text();
        parts.forEach(builder::append);
        return builder.build();
    }

    // ── Internal: plain text (legacy + hex) ───────────────────────────────────

    /**
     * Handles a segment that contains no gradient tags.
     * Translates {@code &} legacy codes and {@code &#RRGGBB} inline hex.
     */
    private static Component parsePlain(String text) {
        // Translate &#RRGGBB into a sequence we can handle char-by-char
        // Strategy: split on hex tags, build components with explicit TextColor
        Matcher hm = HEX_PATTERN.matcher(text);
        if (!hm.find()) {
            // No hex — fast path via LegacyComponentSerializer
            return legacy(text);
        }

        TextComponent.Builder builder = Component.text();
        int cursor = 0;
        hm.reset();

        while (hm.find()) {
            // Legacy segment before this hex tag
            if (hm.start() > cursor) {
                builder.append(legacy(text.substring(cursor, hm.start())));
            }
            // Consume the hex tag — collect the plain text that follows until
            // the next & code or end of string, and colour it.
            TextColor color = hexColor(hm.group(1));
            // Everything after this tag up to the next & code belongs to this color
            int afterTag = hm.end();
            // Find the next & code position
            int nextCode = findNextCode(text, afterTag);
            String colored = text.substring(afterTag, nextCode);
            // The colored text may itself have legacy formatting codes (bold etc.)
            // We parse it as legacy and then apply the hex color on top
            builder.append(legacy(colored).color(color));
            cursor = nextCode;
            // Reposition matcher
            hm.region(cursor, text.length());
        }

        // Remaining text after last hex tag
        if (cursor < text.length()) {
            builder.append(legacy(text.substring(cursor)));
        }

        return builder.build();
    }

    /**
     * Finds the index of the next {@code &X} code in {@code text} starting
     * from {@code from}, or {@code text.length()} if none.
     */
    private static int findNextCode(String text, int from) {
        for (int i = from; i < text.length() - 1; i++) {
            if (text.charAt(i) == '&') return i;
        }
        return text.length();
    }

    // ── Internal: gradient ────────────────────────────────────────────────────

    /**
     * Builds a gradient component by assigning interpolated colours to each
     * visible character in {@code body}.
     *
     * @param stopsStr comma/colon-separated hex colour stops, e.g. {@code #FF0000:#0000FF}
     * @param body     the text to colour — may contain legacy formatting codes
     */
    private static Component buildGradient(String stopsStr, String body) {
        List<Color> stops = parseStops(stopsStr);
        if (stops.size() < 2) {
            // Degenerate — treat as solid colour
            return parsePlain(
                    (stops.isEmpty() ? "" : ("&#" + Integer.toHexString(stops.get(0).getRGB()).substring(2)))
                            + body);
        }

        // Strip legacy codes to count visible characters
        String visible = body.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
        int len = visible.length();
        if (len == 0) return Component.empty();

        TextComponent.Builder builder = Component.text();

        // Track legacy formatting state as we walk through the original body
        boolean bold      = false;
        boolean italic    = false;
        boolean underline = false;
        boolean strike    = false;
        boolean obfuscate = false;

        int visIdx = 0; // index into the visible character sequence
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);

            // Handle & codes — update formatting state, don't emit a character
            if (c == '&' && i + 1 < body.length()) {
                char code = Character.toLowerCase(body.charAt(i + 1));
                switch (code) {
                    case 'l' -> bold      = true;
                    case 'o' -> italic    = true;
                    case 'n' -> underline = true;
                    case 'm' -> strike    = true;
                    case 'k' -> obfuscate = true;
                    case 'r' -> { bold = italic = underline = strike = obfuscate = false; }
                }
                i++; // skip the code char
                continue;
            }

            // Interpolate colour for this visible character
            float t = len == 1 ? 0f : (float) visIdx / (len - 1);
            Color c1 = interpolateStops(stops, t);
            TextColor tc = TextColor.color(c1.getRed(), c1.getGreen(), c1.getBlue());

            var charComp = Component.text(String.valueOf(c)).color(tc);
            if (bold)      charComp = charComp.decorate(TextDecoration.BOLD);
            if (italic)    charComp = charComp.decorate(TextDecoration.ITALIC);
            if (underline) charComp = charComp.decorate(TextDecoration.UNDERLINED);
            if (strike)    charComp = charComp.decorate(TextDecoration.STRIKETHROUGH);
            if (obfuscate) charComp = charComp.decorate(TextDecoration.OBFUSCATED);

            builder.append(charComp);
            visIdx++;
        }

        return builder.build();
    }

    private static List<Color> parseStops(String raw) {
        List<Color> stops = new ArrayList<>();
        for (String part : raw.split("[:#]+")) {
            if (part.isEmpty()) continue;
            try {
                stops.add(Color.decode("#" + part.replace("#", "")));
            } catch (NumberFormatException ignored) {}
        }
        return stops;
    }

    /**
     * Interpolates a position {@code t} (0.0–1.0) across multiple colour stops.
     */
    private static Color interpolateStops(List<Color> stops, float t) {
        if (stops.size() == 1) return stops.get(0);
        float scaledT  = t * (stops.size() - 1);
        int   segment  = Math.min((int) scaledT, stops.size() - 2);
        float local    = scaledT - segment;
        return blend(stops.get(segment), stops.get(segment + 1), local);
    }

    private static Color blend(Color a, Color b, float t) {
        return new Color(
                lerp(a.getRed(),   b.getRed(),   t),
                lerp(a.getGreen(), b.getGreen(), t),
                lerp(a.getBlue(),  b.getBlue(),  t));
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Component legacy(String s) {
        if (s == null || s.isEmpty()) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    private static TextColor hexColor(String hex) {
        try {
            Color c = Color.decode(hex);
            return TextColor.color(c.getRed(), c.getGreen(), c.getBlue());
        } catch (NumberFormatException e) {
            return TextColor.color(0xFFFFFF);
        }
    }
}
