package com.summitcraft.summitraffle.logging;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * Writes structured raffle events to daily rotating log files.
 *
 * <p>Files are written to {@code /plugins/SummitRaffle/logs/YYYY-MM-DD.txt}.
 * A new file is created automatically each day — no manual rotation needed.</p>
 *
 * <p>All writes are synchronous and append-only. The file handle is opened and
 * closed per write so the file is never locked and can be read externally at
 * any time (e.g. tail -f on a Linux host).</p>
 */
public class LogManager {

    private static final DateTimeFormatter DATE_FMT      = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final File logsDir;
    private final java.util.logging.Logger serverLogger;

    public LogManager(JavaPlugin plugin) {
        this.logsDir      = new File(plugin.getDataFolder(), "logs");
        this.serverLogger = plugin.getLogger();
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            serverLogger.warning("Could not create logs directory: " + logsDir.getAbsolutePath());
        }
    }

    // ── Public events ─────────────────────────────────────────────────────────

    /**
     * Logs a raffle start.
     *
     * @param starterName  display name of the player who started it
     * @param starterUUID  their UUID
     * @param prizeName    human-readable prize description (e.g. "64x Diamond")
     * @param durationSecs configured duration in seconds
     */
    public void logRaffleStart(String starterName, UUID starterUUID,
                               String prizeName, int durationSecs) {
        write(String.format(
                "[RAFFLE START] Player: %s (%s) | Prize: %s | Duration: %ds",
                starterName, starterUUID, prizeName, durationSecs));
    }

    /**
     * Logs a raffle end with full outcome.
     *
     * @param prizeName      human-readable prize description
     * @param winnerName     display name of winner, or {@code null} if nobody entered
     * @param winnerUUID     winner UUID, or {@code null} if nobody entered
     * @param participants   full set of participant UUIDs (may be empty)
     * @param winnerOnline   whether the winner was online at draw time
     */
    public void logRaffleEnd(String prizeName,
                             String winnerName, UUID winnerUUID,
                             Set<UUID> participants,
                             boolean winnerOnline) {
        String outcome;
        if (winnerUUID == null) {
            outcome = "NO_WINNER (0 participants)";
        } else {
            outcome = String.format("WINNER: %s (%s) [online=%b]", winnerName, winnerUUID, winnerOnline);
        }
        write(String.format(
                "[RAFFLE END]   Prize: %s | Participants: %d | %s | All entrants: [%s]",
                prizeName,
                participants.size(),
                outcome,
                joinUUIDs(participants)));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void write(String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String line = "[" + timestamp + "] " + message;

        File logFile = new File(logsDir, LocalDate.now().format(DATE_FMT) + ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            serverLogger.warning("Failed to write to raffle log: " + e.getMessage());
        }
    }

    private static String joinUUIDs(Set<UUID> uuids) {
        if (uuids.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (UUID uuid : uuids) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(uuid);
        }
        return sb.toString();
    }
}
