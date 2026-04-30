package com.summitcraft.summitraffle.command;

/**
 * Central store for all player-facing messages.
 */
public final class Messages {

    private Messages() {}

    private static final String PREFIX = "§6[SummitRaffle] §r";
    private static final String RED    = "§c";
    private static final String GREEN  = "§a";
    private static final String YELLOW = "§e";
    private static final String GRAY   = "§7";

    // General
    public static final String USAGE =
            PREFIX + GRAY + "Usage: /raffle <start|join>";

    public static final String NO_PERMISSION =
            PREFIX + RED + "You don't have permission to do that.";

    public static final String PLAYERS_ONLY =
            PREFIX + RED + "Only players can use this command.";

    // Start guards
    public static final String RAFFLE_ALREADY_ACTIVE =
            PREFIX + RED + "A raffle is already running!";

    public static final String MUST_HOLD_ITEM =
            PREFIX + RED + "Hold the item you want to raffle in your main hand.";

    // Join feedback
    public static final String JOIN_SUCCESS =
            PREFIX + GREEN + "You have entered the raffle. Good luck!";

    public static final String ALREADY_JOINED =
            PREFIX + YELLOW + "You have already entered this raffle.";

    public static final String NO_ACTIVE_RAFFLE =
            PREFIX + RED + "There is no active raffle right now.";

    // Dynamic broadcasts
    public static String raffleStarted(String prizeName) {
        return PREFIX + GREEN + "A raffle has started! Prize: " + YELLOW + prizeName
                + GREEN + " — type " + YELLOW + "/raffle join" + GREEN + " to enter!";
    }

    public static String raffleCountdown(String prizeName, int secondsLeft) {
        return PREFIX + YELLOW + secondsLeft + "s" + GRAY + " remaining to join the raffle for "
                + YELLOW + prizeName + GRAY + "!";
    }

    public static String raffleClosed(String prizeName, int participantCount) {
        return PREFIX + RED + "The raffle for " + YELLOW + prizeName
                + RED + " has closed with " + YELLOW + participantCount
                + RED + " participant(s). Drawing winner...";
    }
}
