package com.summitcraft.summitraffle.command;

import com.summitcraft.summitraffle.config.ConfigManager;
import com.summitcraft.summitraffle.util.ColorParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Thin facade over {@link ConfigManager} that builds player-facing strings and
 * Adventure Components.
 *
 * <p>All text originates from config.yml. Strings may contain:</p>
 * <ul>
 *   <li>{@code &6}, {@code &l} etc. — legacy colour/format codes</li>
 *   <li>{@code &#RRGGBB} — inline hex colour</li>
 *   <li>{@code <gradient:#HEX1:#HEX2>text</gradient>} — gradient text</li>
 * </ul>
 *
 * <h3>Centering</h3>
 * <p>Uses Minecraft's per-character pixel widths. Chat width = 320px.</p>
 */
public final class Messages {

    private Messages() {}

    private static ConfigManager config;

    public static void init(ConfigManager configManager) {
        config = configManager;
    }

    // ── Character width table ─────────────────────────────────────────────────

    private static int charWidth(char c) {
        return switch (c) {
            case 'f', 'i', 'l', 't', ' '                     -> 4;
            case 'k'                                          -> 5;
            case 'I', '!', ',', '.', ':', ';', '|', '\'', '`' -> 2;
            case '"'                                          -> 5;
            case '(', ')', '*', '7'                           -> 6;
            case '\u2019'                                     -> 5;
            default                                           -> 6;
        };
    }

    private static int measureWidth(String text) {
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'l') bold = true;
                if (code == 'r') bold = false;
                i++;
                continue;
            }
            // Skip hex inline sequences (§x§R§R§G§G§B§B)
            if (c == '§' && i + 1 < text.length() && text.charAt(i + 1) == 'x') {
                i += 13; // §x + 6×(§+hex digit)
                continue;
            }
            int w = charWidth(c);
            if (bold) w++;
            width += w + 1;
        }
        return width;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Fetches raw message, substitutes named placeholders, returns raw string. */
    private static String raw(String key, String... kv) {
        String s = config.getRawMessage(key);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s = s.replace("{" + kv[i] + "}", kv[i + 1]);
        }
        return s;
    }

    /**
     * Returns a legacy §-coded string for use with {@code sender.sendMessage(String)}.
     * Hex and gradient tags are stripped gracefully (gradients are not supported
     * in plain-string messages — use Component variants for those).
     */
    private static String str(String key, String... kv) {
        // For per-player feedback we serialize via legacy so it stays as a String.
        // Gradient tags are left as-is by legacy serializer (they come through as text),
        // so we strip them to avoid visible tag text in plain messages.
        String r = raw(key, kv);
        r = r.replaceAll("(?i)<gradient:[^>]+>", "").replaceAll("(?i)</gradient>", "");
        return LegacyComponentSerializer.legacyAmpersand().serialize(
                ColorParser.parse(r));
    }

    /** Parses a raw config string into a full Adventure Component. */
    private static Component comp(String key, String... kv) {
        return ColorParser.parse(raw(key, kv));
    }

    /** Parses and centers a Component in the 320px chat window. */
    private static Component centered(String key, String... kv) {
        String r = raw(key, kv);
        // Measure stripping all codes
        String plain = r.replaceAll("(?i)<gradient:[^>]+>|</gradient>", "")
                        .replaceAll("&[0-9a-fk-orA-FK-OR#](?:[0-9a-fA-F]{6})?", "");
        int spaces = Math.max(0, (320 - measureWidth(plain) * 2) / 2 / 4);
        Component pad = Component.text(" ".repeat(spaces));
        return pad.append(ColorParser.parse(r));
    }

    /** Centers a pre-built Component using a plain-text reference for width. */
    private static Component centeredComp(String plainRef, Component built) {
        String plain = plainRef.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
        int spaces = Math.max(0, (320 - measureWidth(plain)) / 2 / 4);
        return Component.text(" ".repeat(spaces)).append(built);
    }

    // ── Per-player feedback (String) ──────────────────────────────────────────

    public static String usage()               { return str("usage"); }
    public static String noPermission()        { return str("no-permission"); }
    public static String playersOnly()         { return str("players-only"); }
    public static String raffleAlreadyActive() { return str("raffle-already-active"); }
    public static String mustHoldItem()        { return str("must-hold-item"); }
    public static String joinSuccess()         { return str("join-success"); }
    public static String alreadyJoined()       { return str("already-joined"); }
    public static String creatorCannotJoin()   { return str("creator-cannot-join"); }
    public static String noActiveRaffle()      { return str("no-active-raffle"); }
    public static String configReloaded()      { return str("config-reloaded"); }
    public static String onCooldown(int s)     { return str("on-cooldown", "seconds", String.valueOf(s)); }
    public static String prizeReturnedToCreator(String p) { return str("prize-returned", "prize", p); }
    public static String inventoryFullItemDropped(String i) { return str("inventory-full-drop", "item", i); }
    public static String pendingPrizeReceived(String p)    { return str("pending-prize-received", "prize", p); }
    public static String pendingPrizeFull(String i)        { return str("pending-prize-full", "prize", i); }

    // ── Broadcast Components ──────────────────────────────────────────────────

    public static Component raffleStartedComponent(String prizeName, String starterName) {
        String hoverRaw  = raw("announce-join-hover");
        String buttonRaw = raw("announce-join-button");
        // Strip codes for width measurement
        String buttonPlain = buttonRaw.replaceAll("&[0-9a-fk-orA-FK-OR]|(?i)<gradient:[^>]+>|</gradient>", "");

        Component joinButton = ColorParser.parse(buttonRaw)
                .clickEvent(ClickEvent.runCommand("/raffle join"))
                .hoverEvent(HoverEvent.showText(ColorParser.parse(hoverRaw)));

        return Component.empty()
                .append(Component.newline())
                .append(centered("announce-separator")).append(Component.newline())
                .append(centered("announce-header")).append(Component.newline())
                .append(centered("announce-prize",      "prize",  prizeName)).append(Component.newline())
                .append(centered("announce-started-by", "player", starterName)).append(Component.newline())
                .append(Component.newline())
                .append(centeredComp(buttonPlain, joinButton)).append(Component.newline())
                .append(Component.newline())
                .append(centered("announce-warning")).append(Component.newline())
                .append(centered("announce-separator")).append(Component.newline());
    }

    public static Component raffleCountdownComponent(String prizeName, int secondsLeft) {
        String hoverRaw = raw("countdown-hover");
        String lineRaw  = raw("countdown", "seconds", String.valueOf(secondsLeft), "prize", prizeName);
        return ColorParser.parse(lineRaw)
                .clickEvent(ClickEvent.runCommand("/raffle join"))
                .hoverEvent(HoverEvent.showText(ColorParser.parse(hoverRaw)));
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

    public static Component raffleCancelledComponent(String prize, String cancellerName) {
        return comp("raffle-cancelled", "prize", prize, "player", cancellerName);
    }

    public static String raffleCancelled(String prize, String cancellerName) {
        return str("raffle-cancelled", "prize", prize, "player", cancellerName);
    }
}
