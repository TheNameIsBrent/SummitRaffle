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
 * <p>All text originates from config.yml — nothing is hard-coded here except
 * the Adventure event wiring (click/hover) which cannot be expressed in plain
 * strings.</p>
 *
 * <h3>Centering</h3>
 * <p>Lines intended for centered display are padded with spaces using a fixed
 * chat-pixel budget (Minecraft's default chat width is 320px; a regular space
 * is 4px wide). This is a best-effort approximation — proportional fonts and
 * chat scaling mean exact centering is impossible server-side.</p>
 */
public final class Messages {

    private Messages() {}

    private static ConfigManager config;

    /** Called once on enable (and again on reload) by the main plugin class. */
    public static void init(ConfigManager configManager) {
        config = configManager;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Fetches a message, replaces {prefix}, and substitutes key=value pairs. */
    private static String msg(String key, String... replacements) {
        String s = config.getMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            s = s.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return s;
    }

    /** Converts a §-coded string to an Adventure Component. */
    private static Component comp(String key, String... replacements) {
        return LegacyComponentSerializer.legacySection().deserialize(msg(key, replacements));
    }

    /**
     * Pads a §-coded string with leading spaces so it appears roughly centered
     * in the default 320px Minecraft chat window.
     * Each regular space character ≈ 4px; average character ≈ 6px.
     */
    private static Component centered(String key, String... replacements) {
        String text = msg(key, replacements);
        // Strip §-codes to measure plain length
        String plain = text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        int chatWidth = 320;
        int textWidth = plain.length() * 6; // rough average px per char
        int padding = Math.max(0, (chatWidth - textWidth) / 2) / 4; // spaces (4px each)
        String padded = " ".repeat(padding) + text;
        return LegacyComponentSerializer.legacySection().deserialize(padded);
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

    /** Centered opening announcement with prize, starter, and clickable join button. */
    public static Component raffleStartedComponent(String prizeName, String starterName) {
        String hoverText  = msg("announce-join-hover");
        String buttonText = msg("announce-join-button");

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
                .append(centered(buttonText, joinButton))
                .append(Component.newline())
                .append(Component.newline())
                .append(centered("announce-warning")).append(Component.newline())
                .append(centered("announce-separator"))
                .append(Component.newline());
    }

    /** Countdown reminder with clickable join text. */
    public static Component raffleCountdownComponent(String prizeName, int secondsLeft) {
        String hoverText   = msg("countdown-hover");
        String full = msg("countdown", "seconds", String.valueOf(secondsLeft), "prize", prizeName);

        // Make the whole line clickable
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

    /** Centered winner announcement panel. */
    public static Component raffleWinner(String winnerName, String prizeName) {
        return Component.empty()
                .append(Component.newline())
                .append(centered("winner-separator")).append(Component.newline())
                .append(centered("winner-header")).append(Component.newline())
                .append(centered("winner-name",   "player", winnerName)).append(Component.newline())
                .append(centered("winner-prize",  "prize",  prizeName)).append(Component.newline())
                .append(centered("winner-congrats")).append(Component.newline())
                .append(centered("winner-separator"))
                .append(Component.newline());
    }

    public static Component raffleNoParticipants(String prizeName) {
        return comp("no-participants", "prize", prizeName);
    }

    // ── Private overload for pre-built components ────────────────────────────

    /** Centers a pre-built component by prepending a space-padding component. */
    private static Component centered(String rawText, Component prebuilt) {
        String plain = rawText.replaceAll("§[0-9a-fk-orA-FK-OR]|&[0-9a-fk-orA-FK-OR]", "");
        int padding = Math.max(0, (320 - plain.length() * 6) / 2) / 4;
        return Component.text(" ".repeat(padding)).append(prebuilt);
    }
}
