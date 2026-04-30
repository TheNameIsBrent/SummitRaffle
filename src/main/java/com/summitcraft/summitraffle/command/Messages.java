package com.summitcraft.summitraffle.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Central store for all player-facing messages.
 *
 * <p>Simple feedback strings use legacy §-codes for brevity.
 * Rich broadcasts (start announcement, countdown, close) use the
 * Adventure API so they can carry click/hover events.</p>
 */
public final class Messages {

    private Messages() {}

    // ── Legacy §-code constants (simple per-player feedback) ─────────────────

    private static final String PREFIX = "§6[SummitRaffle] §r";
    private static final String RED    = "§c";
    private static final String GREEN  = "§a";
    private static final String YELLOW = "§e";
    private static final String GRAY   = "§7";

    public static final String USAGE =
            PREFIX + GRAY + "Usage: /raffle <start|join>";

    public static final String NO_PERMISSION =
            PREFIX + RED + "You don't have permission to do that.";

    public static final String PLAYERS_ONLY =
            PREFIX + RED + "Only players can use this command.";

    public static final String RAFFLE_ALREADY_ACTIVE =
            PREFIX + RED + "A raffle is already running!";

    public static final String MUST_HOLD_ITEM =
            PREFIX + RED + "Hold the item you want to raffle in your main hand.";

    public static final String JOIN_SUCCESS =
            PREFIX + GREEN + "You have entered the raffle. Good luck!";

    public static final String ALREADY_JOINED =
            PREFIX + YELLOW + "You have already entered this raffle.";

    public static final String CREATOR_CANNOT_JOIN =
            PREFIX + RED + "You cannot join your own raffle.";

    public static final String NO_ACTIVE_RAFFLE =
            PREFIX + RED + "There is no active raffle right now.";

    // ── Adventure Components (rich server-wide broadcasts) ───────────────────

    /**
     * Large opening announcement with a clickable join button.
     *
     * <pre>
     * ══════════════════════════════════
     *        ✦ RAFFLE STARTED ✦
     *   Prize: 64x Diamond
     *
     *   [ Click here to join! ]       ← runs /raffle join, hover text
     *
     *   ⚠ Make sure you have a free
     *     inventory slot to receive
     *     your prize!
     * ══════════════════════════════════
     * </pre>
     */
    public static Component raffleStartedComponent(String prizeName) {
        Component separator = Component.text("══════════════════════════════════")
                .color(NamedTextColor.GOLD);

        Component header = Component.text("        ✦ RAFFLE STARTED ✦")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD);

        Component prizeLabel = Component.empty()
                .append(Component.text("  Prize: ").color(NamedTextColor.GRAY))
                .append(Component.text(prizeName).color(NamedTextColor.AQUA)
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
                .append(Component.newline())
                .append(joinButton).append(Component.newline())
                .append(Component.newline())
                .append(warning).append(Component.newline())
                .append(separator)
                .append(Component.newline());
    }

    /** Countdown reminder — broadcast at 30s, 20s, 10s, 5-1s. */
    public static Component raffleCountdownComponent(String prizeName, int secondsLeft) {
        NamedTextColor timeColor = secondsLeft <= 5
                ? NamedTextColor.RED
                : NamedTextColor.YELLOW;

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

    /** Broadcast when the raffle entry window closes and a winner is about to be drawn. */
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
}
