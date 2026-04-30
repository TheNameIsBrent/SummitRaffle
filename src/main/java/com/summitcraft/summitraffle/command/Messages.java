package com.summitcraft.summitraffle.command;

/**
 * Central store for all player-facing messages.
 *
 * <p>Keeping strings here (rather than scattered across command handlers)
 * makes future localisation or config-driven messages straightforward.</p>
 */
public final class Messages {

    private Messages() {}

    // Colour shorthand
    private static final String PREFIX  = "§6[SummitRaffle] §r";
    private static final String RED     = "§c";
    private static final String GREEN   = "§a";
    private static final String YELLOW  = "§e";
    private static final String GRAY    = "§7";

    // General
    public static final String USAGE =
            PREFIX + GRAY + "Usage: /raffle <start|join>";

    public static final String START_USAGE =
            PREFIX + RED + "Usage: /raffle start <prize>";

    public static final String NO_PERMISSION =
            PREFIX + RED + "You don't have permission to do that.";

    public static final String PLAYERS_ONLY =
            PREFIX + RED + "Only players can use this command.";

    // Raffle state
    public static final String RAFFLE_ALREADY_ACTIVE =
            PREFIX + RED + "A raffle is already running!";

    public static final String NO_ACTIVE_RAFFLE =
            PREFIX + RED + "There is no active raffle right now.";

    // Join
    public static final String JOIN_SUCCESS =
            PREFIX + GREEN + "You have entered the raffle. Good luck!";

    public static final String ALREADY_JOINED =
            PREFIX + YELLOW + "You have already entered this raffle.";

    // Dynamic
    public static String raffleStarted(String prize) {
        return PREFIX + GREEN + "A raffle has started! Prize: " + YELLOW + prize
                + GREEN + " — type " + YELLOW + "/raffle join" + GREEN + " to enter!";
    }
}
