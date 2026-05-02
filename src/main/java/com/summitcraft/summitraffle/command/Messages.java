package com.summitcraft.summitraffle.command;

import com.summitcraft.summitraffle.config.ConfigManager;
import com.summitcraft.summitraffle.util.ColorParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * All player-facing messages, sourced from config.yml and rendered via
 * {@link ColorParser}.
 *
 * <p>Broadcast methods that show the prize name accept a {@link Component}
 * so the item's original colours (from its NBT display name) are preserved
 * exactly — no §-code stripping or corruption.</p>
 */
public final class Messages {

    private Messages() {}

    private static ConfigManager config;

    public static void init(ConfigManager configManager) {
        config = configManager;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String raw(String key, String... kv) {
        String s = config.getRawMessage(key);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s = s.replace("{" + kv[i] + "}", kv[i + 1]);
        }
        return s;
    }

    private static Component comp(String key, String... kv) {
        return ColorParser.parse(raw(key, kv));
    }

    /**
     * Builds a Component from a config key, but substitutes {prize} with a
     * pre-built prize Component rather than a plain string — preserving colours.
     *
     * <p>Strategy: split the raw string on the literal "{prize}" placeholder,
     * parse each half, and sandwich the prize Component between them.</p>
     */
    private static Component compWithPrize(String key, Component prizeComp, String... extras) {
        String r = config.getRawMessage(key);
        // Apply any extra substitutions first (e.g. {player}, {count})
        for (int i = 0; i + 1 < extras.length; i += 2) {
            r = r.replace("{" + extras[i] + "}", extras[i + 1]);
        }

        // Split on {prize} and sandwich the Component between
        int idx = r.indexOf("{prize}");
        if (idx < 0) return ColorParser.parse(r); // no {prize} in this message

        String before = r.substring(0, idx);
        String after  = r.substring(idx + "{prize}".length());

        return ColorParser.parse(before).append(prizeComp).append(ColorParser.parse(after));
    }

    // ── Per-player feedback ───────────────────────────────────────────────────

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
    public static Component onCooldown(int s)     { return comp("on-cooldown", "seconds", String.valueOf(s)); }

    public static Component prizeReturnedToCreator(Component prize) {
        return compWithPrize("prize-returned", prize);
    }
    public static Component inventoryFullItemDropped(Component prize) {
        return compWithPrize("inventory-full-drop", prize);
    }
    public static Component pendingPrizeReceived(Component prize) {
        return compWithPrize("pending-prize-received", prize);
    }
    public static Component pendingPrizeFull(Component prize) {
        return compWithPrize("pending-prize-full", prize);
    }
    public static Component raffleCancelled(Component prize, String by) {
        return compWithPrize("raffle-cancelled", prize, "player", by);
    }

    // ── Broadcast Components ──────────────────────────────────────────────────

    public static Component raffleStartedComponent(Component prizeName, String starterName) {
        String hoverRaw  = raw("announce-join-hover");
        String buttonRaw = raw("announce-join-button");

        Component joinButton = ColorParser.parse(buttonRaw)
                .clickEvent(ClickEvent.runCommand("/raffle join"))
                .hoverEvent(HoverEvent.showText(ColorParser.parse(hoverRaw)));

        return Component.empty()
                .append(Component.newline())
                .append(comp("announce-separator")).append(Component.newline())
                .append(comp("announce-header")).append(Component.newline())
                .append(prizeLineComponent("announce-prize", prizeName)).append(Component.newline())
                .append(comp("announce-started-by", "player", starterName)).append(Component.newline())
                .append(Component.newline())
                .append(joinButton).append(Component.newline())
                .append(Component.newline())
                .append(comp("announce-warning")).append(Component.newline())
                .append(comp("announce-separator")).append(Component.newline());
    }

    public static Component raffleCountdownComponent(Component prizeName, int secondsLeft) {
        String hoverRaw = raw("countdown-hover");
        // Build countdown with prize Component embedded
        Component line = compWithPrize("countdown", prizeName, "seconds", String.valueOf(secondsLeft));
        return line
                .clickEvent(ClickEvent.runCommand("/raffle join"))
                .hoverEvent(HoverEvent.showText(ColorParser.parse(hoverRaw)));
    }

    public static Component raffleClosedComponent(Component prizeName, int count) {
        return compWithPrize("entries-closed", prizeName, "count", String.valueOf(count));
    }

    public static Component raffleDrawing() {
        return comp("drawing");
    }

    public static Component raffleWinner(String winnerName, Component prizeName) {
        return Component.empty()
                .append(Component.newline())
                .append(comp("winner-separator")).append(Component.newline())
                .append(comp("winner-header")).append(Component.newline())
                .append(comp("winner-name", "player", winnerName)).append(Component.newline())
                .append(prizeLineComponent("winner-prize", prizeName)).append(Component.newline())
                .append(comp("winner-congrats")).append(Component.newline())
                .append(comp("winner-separator")).append(Component.newline());
    }

    public static Component raffleNoParticipants(Component prizeName) {
        return compWithPrize("no-participants", prizeName);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds a prize line that is centered. The centering measurement uses the
     * plain-text length of the surrounding config text plus the plain-text length
     * of the prize name — so the measurement is accurate even for coloured items.
     */
    private static Component prizeLineComponent(String key, Component prizeComp) {
        String r = config.getRawMessage(key);
        int idx = r.indexOf("{prize}");

        if (idx < 0) return comp(key);

        // For centering: measure the template with plain prize name substituted
        String plainPrize = PlainTextComponentSerializer.plainText().serialize(prizeComp);
        String forMeasure = r.replace("{prize}", plainPrize);
        // Strip <center> tag from measurement string
        forMeasure = forMeasure.replaceAll("(?i)^\\s*<center>|</center>\\s*$", "");

        // Build actual component
        String before = r.substring(0, idx)
                .replaceAll("(?i)^\\s*<center>", "");
        String after  = r.substring(idx + "{prize}".length())
                .replaceAll("(?i)</center>\\s*$", "");

        Component full = ColorParser.parse(before)
                .append(prizeComp)
                .append(ColorParser.parse(after));

        // Only center if the config key had <center>
        if (r.matches("(?i)\\s*<center>.*</center>\\s*")) {
            return ColorParser.centerComponent(full, forMeasure);
        }
        return full;
    }
}
