package com.summitcraft.summitraffle.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.Style;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses color-rich message strings into Adventure {@link Component}s.
 *
 * <h2>Supported syntax (all combinable in one string)</h2>
 * <pre>
 *   &amp;6  &amp;a  &amp;l  &amp;r        — legacy colour / format codes
 *   &amp;#FF5733                  — inline hex colour
 *   &lt;gradient:#FF0000:#0000FF&gt;text&lt;/gradient&gt;  — gradient
 *   &lt;gradient:#F00:#FF0:#0F0&gt;text&lt;/gradient&gt;   — multi-stop gradient
 * </pre>
 */
public final class ColorParser {

    private ColorParser() {}

    // Matches a full gradient block (non-greedy body so nested tags work)
    private static final Pattern GRADIENT =
            Pattern.compile("(?i)<gradient:(#[0-9A-Fa-f]{6}(?::#[0-9A-Fa-f]{6})+)>(.*?)</gradient>",
                    Pattern.DOTALL);

    /** Matches a <center> wrapper around any content */
    private static final Pattern CENTER =
            Pattern.compile("(?i)^\\s*<center>(.*)</center>\\s*$", Pattern.DOTALL);

    /** Usable chat pixel width. Minecraft default chat is 320px minus 1px border each side. */
    private static final int CHAT_WIDTH_PX = 318;
    /** A regular space glyph is 3px + 1px shadow = 4px total. */
    private static final int SPACE_WIDTH_PX = 4;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parses a raw config string into a fully styled Adventure Component.
     *
     * <p>Supported syntax:</p>
     * <ul>
     *   <li>{@code &6}, {@code &l}, {@code &r} — legacy colour/format codes</li>
     *   <li>{@code &#RRGGBB} — inline hex colour</li>
     *   <li>{@code <gradient:#HEX1:#HEX2>text</gradient>} — gradient</li>
     *   <li>{@code <center>text</center>} — centers the line in chat</li>
     * </ul>
     * All syntaxes can be combined freely.
     */
    public static Component parse(String input) {
        if (input == null || input.isEmpty()) return Component.empty();

        // Handle <center> wrapper — pass raw inner string for accurate width measurement
        Matcher cm = CENTER.matcher(input);
        if (cm.matches()) {
            String inner = cm.group(1);
            return centerComponent(parseInner(inner), inner);
        }

        return parseInner(input);
    }

    /**
     * Measures the pixel width of the plain-text content of a Component
     * and prepends enough space characters to center it in {@link #CHAT_WIDTH_PX}.
     *
     * @param content   the already-parsed Component to center
     * @param rawSource the original raw config string (before parsing) used for
     *                  accurate width measurement — bold/format state is preserved
     */
    public static Component centerComponent(Component content, String rawSource) {
        int textPx = measureRaw(rawSource);
        int spaces = Math.max(0, (CHAT_WIDTH_PX - textPx) / 2 / SPACE_WIDTH_PX);
        return Component.text(" ".repeat(spaces)).append(content);
    }

    /**
     * Convenience overload that measures via plain-text extraction from the Component.
     * Less accurate for bold text — prefer {@link #centerComponent(Component, String)}
     * when the raw source string is available.
     */
    public static Component centerComponent(Component content) {
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(content);
        int textPx = measureWidth(plain, false);
        int spaces = Math.max(0, (CHAT_WIDTH_PX - textPx) / 2 / SPACE_WIDTH_PX);
        return Component.text(" ".repeat(spaces)).append(content);
    }

    /** Internal: parse a string that has already had the <center> wrapper stripped. */
    private static Component parseInner(String input) {
        TextComponent.Builder root = Component.text();

        Matcher gm = GRADIENT.matcher(input);
        int cursor = 0;

        while (gm.find()) {
            if (gm.start() > cursor) {
                parsePlainInto(root, input.substring(cursor, gm.start()), new FormatState());
            }
            root.append(buildGradient(gm.group(1), gm.group(2)));
            cursor = gm.end();
        }

        if (cursor < input.length()) {
            parsePlainInto(root, input.substring(cursor), new FormatState());
        }

        return root.build();
    }

    // ── Character width (for centering) ──────────────────────────────────────

    private static int charWidth(char c) {
        return switch (c) {
            case 'f', 'i', 'l', 't', ' '                      -> 4;
            case 'k'                                           -> 5;
            case 'I', '!', ',', '.', ':', ';', '|', '\'', '`' -> 2;
            case '"'                                           -> 5;
            case '(', ')', '*', '7'                            -> 6;
            case '\u25AC'                                      -> 8; // ▬ solid block
            default                                            -> 6;
        };
    }

    /** Measure pixel width of a plain string with no bold tracking. */
    private static int measureWidth(String plain, boolean bold) {
        int w = 0;
        for (char c : plain.toCharArray()) {
            int cw = charWidth(c);
            if (bold) cw++;        // bold adds 1px per glyph
            w += cw + 1;           // +1 inter-character shadow pixel
        }
        return w;
    }

    /**
     * Measure pixel width of a raw config string, tracking {@code &l}/{@code &r}
     * bold state and stripping all other codes and tags.
     * This is the accurate method — use it whenever the raw source is available.
     */
    private static int measureRaw(String raw) {
        // Strip gradient tags and hex tags — we only care about visible characters
        String s = raw
                .replaceAll("(?i)<gradient:[^>]*>", "")
                .replaceAll("(?i)</gradient>", "")
                .replaceAll("(?i)&#[0-9A-Fa-f]{6}", "");

        int width = 0;
        boolean bold = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < s.length()) {
                char code = Character.toLowerCase(s.charAt(i + 1));
                if (code == 'l') { bold = true;  i++; continue; }
                if (code == 'r') { bold = false; i++; continue; }
                // Any other & code — skip it (colour, format, all non-visible)
                if ("0123456789abcdefkmnor".indexOf(code) >= 0) { i++; continue; }
            }
            int cw = charWidth(c);
            if (bold) cw++;
            width += cw + 1;
        }
        return width;
    }

    /**
     * Walks through a plain (non-gradient) segment character by character,
     * applying legacy codes and hex colour tags to a shared {@link FormatState}.
     */
    private static void parsePlainInto(TextComponent.Builder out,
                                       String text, FormatState state) {
        // We build a list of "tokens": either a &CODE, a &#RRGGBB, or a run of plain chars.
        // This avoids the regex-region repositioning bug in the old code.
        int i = 0;
        StringBuilder pending = new StringBuilder();

        while (i < text.length()) {
            char c = text.charAt(i);

            if (c == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);

                // &#RRGGBB hex tag
                if (next == '#' && i + 7 < text.length()) {
                    String maybeHex = text.substring(i + 2, i + 8);
                    if (maybeHex.matches("[0-9A-Fa-f]{6}")) {
                        // Flush pending text with current state
                        if (!pending.isEmpty()) {
                            out.append(styledText(pending.toString(), state));
                            pending.setLength(0);
                        }
                        state.color = TextColor.color(
                                Integer.parseInt(maybeHex.substring(0, 2), 16),
                                Integer.parseInt(maybeHex.substring(2, 4), 16),
                                Integer.parseInt(maybeHex.substring(4, 6), 16));
                        i += 8; // skip &#RRGGBB
                        continue;
                    }
                }

                // Legacy format / colour code
                char code = Character.toLowerCase(next);
                if ("0123456789abcdefklmnor".indexOf(code) >= 0) {
                    // Flush pending
                    if (!pending.isEmpty()) {
                        out.append(styledText(pending.toString(), state));
                        pending.setLength(0);
                    }
                    state.applyCode(code);
                    i += 2; // skip &X
                    continue;
                }
            }

            pending.append(c);
            i++;
        }

        // Final flush
        if (!pending.isEmpty()) {
            out.append(styledText(pending.toString(), state));
        }
    }

    /** Wraps plain text in a Component styled by the current {@link FormatState}. */
    private static Component styledText(String text, FormatState s) {
        if (text.isEmpty()) return Component.empty();

        Style.Builder style = Style.style();
        if (s.color != null) style.color(s.color);
        if (s.bold)          style.decoration(TextDecoration.BOLD,          true);
        if (s.italic)        style.decoration(TextDecoration.ITALIC,        true);
        if (s.underline)     style.decoration(TextDecoration.UNDERLINED,    true);
        if (s.strike)        style.decoration(TextDecoration.STRIKETHROUGH, true);
        if (s.obfuscate)     style.decoration(TextDecoration.OBFUSCATED,    true);

        return Component.text(text).style(style.build());
    }

    // ── Gradient builder ─────────────────────────────────────────────────────

    private static Component buildGradient(String stopsRaw, String body) {
        List<Color> stops = parseStops(stopsRaw);
        if (stops.size() < 2) {
            // Degenerate — fall back to plain parse
            return parse(body);
        }

        // Count visible (non-code) characters in body
        String visible = body.replaceAll("(?i)&#[0-9A-Fa-f]{6}", "")
                             .replaceAll("(?i)&[0-9a-fk-or]", "");
        int total = visible.length();
        if (total == 0) return Component.empty();

        TextComponent.Builder builder = Component.text();
        FormatState state = new FormatState();
        int visIdx = 0;
        int i = 0;

        while (i < body.length()) {
            char c = body.charAt(i);

            // Handle & codes (including &#hex) — update state, no character emitted
            if (c == '&' && i + 1 < body.length()) {
                char next = body.charAt(i + 1);
                if (next == '#' && i + 7 < body.length()) {
                    String maybeHex = body.substring(i + 2, i + 8);
                    if (maybeHex.matches("[0-9A-Fa-f]{6}")) {
                        // Hex override inside gradient — apply as tint but still gradient-colour
                        // (we ignore it to keep gradient clean; the gradient colour wins)
                        i += 8;
                        continue;
                    }
                }
                char code = Character.toLowerCase(next);
                if ("0123456789abcdefklmnor".indexOf(code) >= 0) {
                    state.applyCode(code);
                    i += 2;
                    continue;
                }
            }

            // Emit this visible character with its gradient colour
            float t  = total == 1 ? 0f : (float) visIdx / (total - 1);
            Color ci = interpolate(stops, t);
            TextColor tc = TextColor.color(ci.getRed(), ci.getGreen(), ci.getBlue());

            Style.Builder style = Style.style().color(tc);
            if (state.bold)      style.decoration(TextDecoration.BOLD,          true);
            if (state.italic)    style.decoration(TextDecoration.ITALIC,        true);
            if (state.underline) style.decoration(TextDecoration.UNDERLINED,    true);
            if (state.strike)    style.decoration(TextDecoration.STRIKETHROUGH, true);
            if (state.obfuscate) style.decoration(TextDecoration.OBFUSCATED,    true);

            builder.append(Component.text(String.valueOf(c)).style(style.build()));
            visIdx++;
            i++;
        }

        return builder.build();
    }

    // ── Colour interpolation ──────────────────────────────────────────────────

    private static List<Color> parseStops(String raw) {
        List<Color> stops = new ArrayList<>();
        for (String part : raw.split(":")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            if (!part.startsWith("#")) part = "#" + part;
            try { stops.add(Color.decode(part)); }
            catch (NumberFormatException ignored) {}
        }
        return stops;
    }

    private static Color interpolate(List<Color> stops, float t) {
        if (stops.size() == 1) return stops.get(0);
        float scaled  = t * (stops.size() - 1);
        int   seg     = Math.min((int) scaled, stops.size() - 2);
        float local   = scaled - seg;
        Color a = stops.get(seg), b = stops.get(seg + 1);
        return new Color(
                lerp(a.getRed(),   b.getRed(),   local),
                lerp(a.getGreen(), b.getGreen(), local),
                lerp(a.getBlue(),  b.getBlue(),  local));
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    // ── Format state tracker ──────────────────────────────────────────────────

    /**
     * Mutable formatting state that mirrors what the legacy &-code system tracks.
     * Shared across a parse pass so bold/italic etc. correctly span across hex tags.
     */
    private static final class FormatState {
        TextColor color     = null;
        boolean   bold      = false;
        boolean   italic    = false;
        boolean   underline = false;
        boolean   strike    = false;
        boolean   obfuscate = false;

        void applyCode(char code) {
            switch (code) {
                // Colours — map to NamedTextColor for the 16 legacy colours
                case '0' -> { color = NamedTextColor.BLACK;        resetFormat(); }
                case '1' -> { color = NamedTextColor.DARK_BLUE;    resetFormat(); }
                case '2' -> { color = NamedTextColor.DARK_GREEN;   resetFormat(); }
                case '3' -> { color = NamedTextColor.DARK_AQUA;    resetFormat(); }
                case '4' -> { color = NamedTextColor.DARK_RED;     resetFormat(); }
                case '5' -> { color = NamedTextColor.DARK_PURPLE;  resetFormat(); }
                case '6' -> { color = NamedTextColor.GOLD;         resetFormat(); }
                case '7' -> { color = NamedTextColor.GRAY;         resetFormat(); }
                case '8' -> { color = NamedTextColor.DARK_GRAY;    resetFormat(); }
                case '9' -> { color = NamedTextColor.BLUE;         resetFormat(); }
                case 'a' -> { color = NamedTextColor.GREEN;        resetFormat(); }
                case 'b' -> { color = NamedTextColor.AQUA;         resetFormat(); }
                case 'c' -> { color = NamedTextColor.RED;          resetFormat(); }
                case 'd' -> { color = NamedTextColor.LIGHT_PURPLE; resetFormat(); }
                case 'e' -> { color = NamedTextColor.YELLOW;       resetFormat(); }
                case 'f' -> { color = NamedTextColor.WHITE;        resetFormat(); }
                // Formats
                case 'k' -> obfuscate = true;
                case 'l' -> bold      = true;
                case 'm' -> strike    = true;
                case 'n' -> underline = true;
                case 'o' -> italic    = true;
                // Reset
                case 'r' -> { color = null; resetFormat(); }
            }
        }

        private void resetFormat() {
            bold = italic = underline = strike = obfuscate = false;
        }
    }
}
