package com.summitcraft.summitraffle.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Central store for all player-facing messages.
 *
 * <p>The prefix is injected at startup (and on reload) from config.yml via
 * {@link #setPrefix(String)}. Per-player feedback uses legacy §-codes for
 * simplicity. Rich server-wide broadcasts use the Adventure API.</p>
 */
public final class Messages {

    private Messages() {}

    // Injected from config.yml on enable
    private static String PREFIX = "§6[SummitRaffle] §r";

    /** Called by the main plugin class after loading config.yml. */
    public static void setPrefix(String prefix) {
        PREFIX = prefix.endsWith(" ") ? prefix : prefix + " ";
    }

    // ── Colour shorthands ─────────────────────────────────────────────────────

    private static final String RED    = "§c";
    private static final String GREEN  = "§a";
    private static final String YELLOW = "§e";
    private static final String GRAY   = "§7";

    // ── Per-player feedback ───────────────────────────────────────────────────

    public static String usage()               { return PREFIX + GRAY   + "Usage: /raffle <start|join>"; }
    public static String noPermission()        { return PREFIX + RED    + "You don't have permission to do that."; }
    public static String playersOnly()         { return PREFIX + RED    + "Only players can use this command."; }
    public static String raffleAlreadyActive() { return PREFIX + RED    + "A raffle is already running!"; }
    public static String mustHoldItem()        { return PREFIX + RED    + "Hold the item you want to raffle in your main hand."; }
    public static String joinSuccess()         { return PREFIX + GREEN  + "You have entered the raffle. Good luck!"; }
    public static String alreadyJoined()       { return PREFIX + YELLOW + "You have already entered this raffle."; }
    public static String creatorCannotJoin()   { return PREFIX + RED    + "You cannot join your own raffle."; }
    public static String noActiveRaffle()      { return PREFIX + RED    + "There is no active raffle right now."; }

    // ── Adventure Components (rich server-wide broadcasts) ───────────────────

    /**
     * Large opening announcement with prize, starter name, and a clickable join button.
     *
     * <pre>
     * ══════════════════════════════════
     *        ✦ RAFFLE STARTED ✦
     *   Prize:      64x Diamond
     *   Started by: Steve
     *
     *       [ Click here to join! ]
     *
     *   ⚠ Make sure you have a free
     *     inventory slot to receive
     *     your prize if you win!
     * ══════════════════════════════════
     * </pre>
     */
    public static Component raffleStartedComponent(String prizeName, String starterName) {
        Component separator = Component.text("══════════════════════════════════")
                .color(NamedTextColor.GOLD);

        Component header = Component.text("        ✦ RAFFLE STARTED ✦")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD);

        Component prizeLabel = Component.empty()
                .append(Component.text("  Prize:      ").color(NamedTextColor.GRAY))
                .append(Component.text(prizeName).color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD));

        Component starterLabel = Component.empty()
                .append(Component.text("  Started by: ").color(NamedTextColor.GRAY))
                .append(Component.text(starterName).color(NamedTextColor.WHITE)
                        .decorate(TextDecoration.BOLD));

        Component joinButton = Component.text("       [ Click here to join! ]")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/raffle join"))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to join the raffle!")
                                .color(NamedTextColor.YELLOW)));

        Component warning = Component.empty()
                .append(Component.text("  ⚠ ").color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("Make sure you have a free inventory slot")
                        .color(NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.text("    to receive your prize if you win!")
                        .color(NamedTextColor.RED));

        return Component.empty()
                .append(Component.newline())
                .append(separator).append(Component.newline())
                .append(header).append(Component.newline())
                .append(prizeLabel).append(Component.newline())
                .append(starterLabel).append(Component.newline())
                .append(Component.newline())
                .append(joinButton).append(Component.newline())
                .append(Component.newline())
                .append(warning).append(Component.newline())
                .append(separator)
                .append(Component.newline());
    }

    /** Countdown reminder — broadcast at 30s, 20s, 10s, 5-1s. */
    public static Component raffleCountdownComponent(String prizeName, int secondsLeft) {
        NamedTextColor timeColor = secondsLeft <= 5 ? NamedTextColor.RED : NamedTextColor.YELLOW;

        return Component.empty()
                .append(Component.text("[Raffle] ").color(NamedTextColor.GOLD))
                .append(Component.text(secondsLeft + "s ").color(timeColor)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("remaining for ").color(NamedTextColor.GRAY))
                .append(Component.text(prizeName).color(NamedTextColor.AQUA))
                .append(Component.text(" — ").color(NamedTextColor.GRAY))
                .append(Component.text("Click to join!")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/raffle join"))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Click to join the raffle!")
                                        .color(NamedTextColor.YELLOW))));
    }

    /** Broadcast when the entry window closes and a winner is about to be drawn. */
    public static Component raffleClosedComponent(String prizeName, int participantCount) {
        return Component.empty()
                .append(Component.text("[Raffle] ").color(NamedTextColor.GOLD))
                .append(Component.text("Entries closed for ").color(NamedTextColor.GRAY))
                .append(Component.text(prizeName).color(NamedTextColor.AQUA))
                .append(Component.text(" — ").color(NamedTextColor.GRAY))
                .append(Component.text(participantCount + " participant(s)")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(". Drawing winner...").color(NamedTextColor.GRAY));
    }

    /** Broadcast when a winner is drawn. */
    public static Component raffleWinner(String winnerName, String prizeName) {
        return Component.empty()
                .append(Component.newline())
                .append(Component.text("  ✦ RAFFLE WINNER ✦  ").color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("  Winner: ").color(NamedTextColor.GRAY))
                .append(Component.text(winnerName).color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("  Prize:  ").color(NamedTextColor.GRAY))
                .append(Component.text(prizeName).color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("  Congratulations!").color(NamedTextColor.GREEN))
                .append(Component.newline());
    }

    /** Broadcast when nobody entered and the item is returned. */
    public static Component raffleNoParticipants(String prizeName) {
        return Component.empty()
                .append(Component.text("[Raffle] ").color(NamedTextColor.GOLD))
                .append(Component.text("The raffle for ").color(NamedTextColor.GRAY))
                .append(Component.text(prizeName).color(NamedTextColor.AQUA))
                .append(Component.text(" ended with no participants. Item returned to the host.")
                        .color(NamedTextColor.GRAY));
    }

    /** Sent privately to the creator when their item is returned. */
    public static String prizeReturnedToCreator(String prizeName) {
        return PREFIX + GREEN + "No one entered — your " + YELLOW + prizeName
                + GREEN + " has been returned to your inventory.";
    }

    /** Sent privately to the winner when their inventory was full and the item dropped. */
    public static String inventoryFullItemDropped(String itemName) {
        return PREFIX + YELLOW + "Your inventory was full! Your prize ("
                + itemName + ") has been dropped at your feet.";
    }
}
