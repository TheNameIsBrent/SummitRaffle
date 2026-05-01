package com.summitcraft.summitraffle.command;

import com.summitcraft.summitraffle.config.ConfigManager;
import com.summitcraft.summitraffle.util.ColorParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * All player-facing messages, sourced from config.yml and rendered via
 * {@link ColorParser}.
 *
 * <p>Every method returns an Adventure {@link Component} — never a raw String.
 * This ensures hex colours and gradients are never accidentally serialized
 * back to the BungeeCord §x§R§R§G§G§B§B format before reaching the player.</p>
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
            case 'f', 'i', 'l', 't', ' '                      -> 4;
            case 'k'                                           -> 5;
            case 'I', '!', ',', '.', ':', ';', '|', '\'', '`' -> 2;
            case '"'                                           -> 5;
            case '(', ')', '*', '7'                            -> 6;
            default                                            -> 6;
        };
    }

    private static int measureWidth(String plain) {
        int w = 0;
        for (char c : plain.toCharArray()) w += charWidth(c) + 1;
        return w;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Fetch raw config string, substitute {key} placeholders. */
    private static String raw(String key, String... kv) {
        String s = config.getRawMessage(key);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s = s.replace("{" + kv[i] + "}", kv[i + 1]);
        }
        return s;
    }

    /** Parse raw string → Component. Used for all messages. */
    private static Component comp(String key, String... kv) {
        return ColorParser.parse(raw(key, kv));
    }

    /**
     * Parse and center a Component in Minecraft's 320px chat window.
     * Strips all codes to measure plain-text width, then pads with spaces.
     */
    private static Component centered(String key, String... kv) {
        String r = raw(key, kv);
        // Strip gradient tags, &codes, &#hex to get plain text for measurement
        String plain = r
                .replaceAll("(?i)<gradient:[^>]*>|</gradient>", "")
                .replaceAll("(?i)&#[0-9A-Fa-f]{6}", "")
                .replaceAll("(?i)&[0-9a-fk-or]", "");
        int spaces = Math.max(0, (320 - measureWidth(plain)) / 2 / 4);
        return Component.text(" ".repeat(spaces)).append(ColorParser.parse(r));
    }

    /** Center a pre-built Component using a raw string as the width reference. */
    private static Component centeredRaw(String rawRef, Component built) {
        String plain = rawRef
                .replaceAll("(?i)<gradient:[^>]*>|</gradient>", "")
                .replaceAll("(?i)&#[0-9A-Fa-f]{6}", "")
                .replaceAll("(?i)&[0-9a-fk-or]", "");
        int spaces = Math.max(0, (320 - measureWidth(plain)) / 2 / 4);
        return Component.text(" ".repeat(spaces)).append(built);
    }

    // ── Per-player feedback ───────────────────────────────────────────────────
    // All return Component so hex/gradient colours reach the client intact.

    public static Component usage()               { return comp("usage"); }
    public static Component noPermission()        { return comp("no-permission"); }
    public static Component playersOnly()         { return comp("players-only"); }
    public static Component raffleAlreadyActive() { return comp("raffle-already-active"); }
    public static Component mustHoldItem()        { return comp("must-hold-item"); }
    public static Component joinSuccess()         { return comp("join-success"); }
    public static Component alreadyJoined()       { return comp("already-joined"); }
    public static Component creatorCannotJoin()   { return comp("creator-cannot-join"); }
    public static Component noActiveRaffle()      { return comp("no-active-raffle"); }
    public static Component configReloaded()      { return comp("config-reloaded"); }
    public static Component onCooldown(int s)     { return comp("on-cooldown",  "seconds", String.valueOf(s)); }
    public static Component prizeReturnedToCreator(String p) { return comp("prize-returned",        "prize", p); }
    public static Component inventoryFullItemDropped(String i){ return comp("inventory-full-drop",   "item",  i); }
    public static Component pendingPrizeReceived(String p)   { return comp("pending-prize-received","prize", p); }
    public static Component pendingPrizeFull(String i)       { return comp("pending-prize-full",    "prize", i); }
    public static Component raffleCancelled(String prize, String by) {
        return comp("raffle-cancelled", "prize", prize, "player", by);
    }

    // ── Broadcast Components ──────────────────────────────────────────────────

    public static Component raffleStartedComponent(String prizeName, String starterName) {
        String hoverRaw  = raw("announce-join-hover");
        String buttonRaw = raw("announce-join-button");

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
                .append(centeredRaw(buttonRaw, joinButton)).append(Component.newline())
                .append(Component.newline())
                .append(centered("announce-warning")).append(Component.newline())
                .append(centered("announce-separator")).append(Component.newline());
    }

    public static Component raffleCountdownComponent(String prizeName, int secondsLeft) {
        String lineRaw  = raw("countdown",       "seconds", String.valueOf(secondsLeft), "prize", prizeName);
        String hoverRaw = raw("countdown-hover");
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
}
