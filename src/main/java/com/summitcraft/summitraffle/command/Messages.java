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
 * <p>Supported in config.yml:</p>
 * <ul>
 *   <li>{@code &6}, {@code &l}, {@code &r} — legacy colour/format codes</li>
 *   <li>{@code &#RRGGBB} — inline hex colour</li>
 *   <li>{@code <gradient:#HEX1:#HEX2>text</gradient>} — gradient</li>
 *   <li>{@code <center>text</center>} — centers the line in chat</li>
 * </ul>
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

    /** Parse → Component. <center>, gradients, hex, & codes all handled. */
    private static Component comp(String key, String... kv) {
        return ColorParser.parse(raw(key, kv));
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
    public static Component onCooldown(int s)     { return comp("on-cooldown",  "seconds", String.valueOf(s)); }
    public static Component prizeReturnedToCreator(String p) { return comp("prize-returned",         "prize", p); }
    public static Component inventoryFullItemDropped(String i){ return comp("inventory-full-drop",    "item",  i); }
    public static Component pendingPrizeReceived(String p)   { return comp("pending-prize-received", "prize", p); }
    public static Component pendingPrizeFull(String i)       { return comp("pending-prize-full",     "prize", i); }
    public static Component raffleCancelled(String prize, String by) {
        return comp("raffle-cancelled", "prize", prize, "player", by);
    }

    // ── Broadcast Components ──────────────────────────────────────────────────

    public static Component raffleStartedComponent(String prizeName, String starterName) {
        String hoverRaw  = raw("announce-join-hover");
        String buttonRaw = raw("announce-join-button");

        // Parse button (center tag handled by ColorParser), attach click/hover events
        Component centeredButton = ColorParser.parse(buttonRaw)
                .clickEvent(ClickEvent.runCommand("/raffle join"))
                .hoverEvent(HoverEvent.showText(ColorParser.parse(hoverRaw)));

        return Component.empty()
                .append(Component.newline())
                .append(comp("announce-separator")).append(Component.newline())
                .append(comp("announce-header")).append(Component.newline())
                .append(comp("announce-prize",      "prize",  prizeName)).append(Component.newline())
                .append(comp("announce-started-by", "player", starterName)).append(Component.newline())
                .append(Component.newline())
                .append(centeredButton).append(Component.newline())
                .append(Component.newline())
                .append(comp("announce-warning")).append(Component.newline())
                .append(comp("announce-separator")).append(Component.newline());
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
                .append(comp("winner-separator")).append(Component.newline())
                .append(comp("winner-header")).append(Component.newline())
                .append(comp("winner-name",    "player", winnerName)).append(Component.newline())
                .append(comp("winner-prize",   "prize",  prizeName)).append(Component.newline())
                .append(comp("winner-congrats")).append(Component.newline())
                .append(comp("winner-separator")).append(Component.newline());
    }

    public static Component raffleNoParticipants(String prizeName) {
        return comp("no-participants", "prize", prizeName);
    }
}
