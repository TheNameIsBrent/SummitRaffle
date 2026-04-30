package com.summitcraft.summitraffle.command;

import com.summitcraft.summitraffle.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Thin facade over {@link ConfigManager} that builds player-facing strings and
 * Adventure Components.
 *
 * <p>All text originates from config.yml. Messages support the {@code {prefix}}
 * placeholder which is substituted at read-time.</p>
 *
 * <h3>Centering</h3>
 * <p>Uses Minecraft's actual per-character pixel widths (default resource pack,
 * normal style). Bold characters add 1px per glyph. Chat width is 320px;
 * a space is 4px wide (3px glyph + 1px shadow). Centering is best-effort —
 * client-side chat scaling can shift things slightly.</p>
 */
public final class Messages {

    private Messages() {}

    private static ConfigManager config;

    public static void init(ConfigManager configManager) {
        config = configManager;
    }

    // ── Character width table (px, normal style, default font) ────────────────
    // Source: Minecraft's font/default.json glyph sizes
    // Bold adds +1px per character (tracked via isBold flag in stripAndMeasure)

    private static int charWidth(char c) {
        return switch (c) {
            case 'f', 'i', 'l', 't', ' '             -> 4;
            case 'k'                                  -> 5;
            case 'I', '!', ',', '.', ':', ';',
                 '|', '\'', '`'                       -> 2;
            case '"'                                  -> 5;
            case '(', ')', '*', '7'                   -> 6;
            case '\u2019'                             -> 5; // right single quote
            default                                   -> 6;
        };
    }

    /**
     * Measures the pixel width of a §-coded string, accounting for bold (+1px/char).
     */
    private static int measureWidth(String text) {
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                bold = code == 'l';
                if (code == 'r') bold = false;
                i++; // skip the code char
                continue;
            }
            int w = charWidth(c);
            if (bold) w++; // bold adds 1px shadow offset
            width += w + 1; // +1 for the inter-character shadow pixel
        }
        return width;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String msg(String key, String... replacements) {
        String s = config.getMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            s = s.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return s;
    }

    private static Component comp(String key, String... replacements) {
        return LegacyComponentSerializer.legacySection().deserialize(msg(key, replacements));
    }

    /**
     * Returns a Component padded so the text is centered in a 320px chat window.
     * Space char = 4px. We subtract 1px from the leading space to account for
     * the shadow pixel on the space itself.
     */
    private static Component centered(String key, String... replacements) {
        String text = msg(key, replacements);
        int textWidth = measureWidth(text);
        int spaces = Math.max(0, (320 - textWidth) / 2 / 4);
        return LegacyComponentSerializer.legacySection()
                .deserialize(" ".repeat(spaces) + text);
    }

    /** Center a pre-built Component by prepending measured spaces from a plain text reference. */
    private static Component centeredComp(String plainRef, Component built) {
        int textWidth = measureWidth(plainRef);
        int spaces = Math.max(0, (320 - textWidth) / 2 / 4);
        return Component.text(" ".repeat(spaces)).append(built);
    }

    // ── Per-player feedback ───────────────────────────────────────────────────

    public static String usage()               { return msg("usage"); }
    public static String noPermission()        { return msg("no-permission"); }
    public static String playersOnly()         { return msg("players-only"); }
    public static String raffleAlreadyActive() { return msg("raffle-already-active"); }
    public static String mustHoldItem()        { return msg("must-hold-item"); }
    public static String joinSuccess()         { return msg("join-success"); }
    public static String alreadyJoined()       { return msg("already-joined"); }
    public static String creatorCannotJoin()   { return msg("creator-cannot-join"); }
    public static String noActiveRaffle()      { return msg("no-active-raffle"); }
    public static String onCooldown(int secs)  { return msg("on-cooldown", "seconds", String.valueOf(secs)); }
    public static String prizeReturnedToCreator(String prize) { return msg("prize-returned", "prize", prize); }
    public static String inventoryFullItemDropped(String item) { return msg("inventory-full-drop", "item", item); }
    public static String pendingPrizeReceived(String prize)   { return msg("pending-prize-received", "prize", prize); }
    public static String pendingPrizeFull(String item)        { return msg("pending-prize-full", "prize", item); }

    // ── Adventure broadcasts ──────────────────────────────────────────────────

    public static Component raffleStartedComponent(String prizeName, String starterName) {
        String hoverText  = msg("announce-join-hover");
        String buttonText = msg("announce-join-button");

        // Strip §-codes from button text to measure its width for centering
        String buttonPlain = buttonText.replaceAll("§[0-9a-fk-orA-FK-OR]", "");

        Component joinButton = LegacyComponentSerializer.legacySection()
                .deserialize(buttonText)
                .clickEvent(ClickEvent.runCommand("/raffle join"))
                .hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacySection().deserialize(hoverText)));

        return Component.empty()
                .append(Component.newline())
                .append(centered("announce-separator")).append(Component.newline())
                .append(centered("announce-header")).append(Component.newline())
                .append(centered("announce-prize",      "prize",  prizeName)).append(Component.newline())
                .append(centered("announce-started-by", "player", starterName)).append(Component.newline())
                .append(Component.newline())
                .append(centeredComp(buttonText, joinButton)).append(Component.newline())
                .append(Component.newline())
                .append(centered("announce-warning")).append(Component.newline())
                .append(centered("announce-separator")).append(Component.newline());
    }

    public static Component raffleCountdownComponent(String prizeName, int secondsLeft) {
        String full      = msg("countdown", "seconds", String.valueOf(secondsLeft), "prize", prizeName);
        String hoverText = msg("countdown-hover");
        return LegacyComponentSerializer.legacySection().deserialize(full)
                .clickEvent(ClickEvent.runCommand("/raffle join"))
                .hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacySection().deserialize(hoverText)));
    }

    public static Component raffleClosedComponent(String prizeName, int count) {
        return comp("entries-closed", "prize", prizeName, "count", String.valueOf(count));
    }

    public static Component raffleDrawing() {
        return comp("drawing");
    }

    public static Component raffleWinner(String winnerName, String prizeName) {
        return Component.empty()
                .append(Component.newline())
                .append(centered("winner-separator")).append(Component.newline())
                .append(centered("winner-header")).append(Component.newline())
                .append(centered("winner-name",    "player", winnerName)).append(Component.newline())
                .append(centered("winner-prize",   "prize",  prizeName)).append(Component.newline())
                .append(centered("winner-congrats")).append(Component.newline())
                .append(centered("winner-separator")).append(Component.newline());
    }

    public static Component raffleNoParticipants(String prizeName) {
        return comp("no-participants", "prize", prizeName);
    }
}
