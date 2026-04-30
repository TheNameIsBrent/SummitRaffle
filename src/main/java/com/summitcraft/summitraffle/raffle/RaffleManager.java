package com.summitcraft.summitraffle.raffle;

import com.summitcraft.summitraffle.command.Messages;
import com.summitcraft.summitraffle.config.ConfigManager;
import com.summitcraft.summitraffle.logging.LogManager;
import com.summitcraft.summitraffle.prize.PendingPrizeManager;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of raffles on the server.
 * Enforces the rule that only ONE raffle may be active at any time.
 */
public class RaffleManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final LogManager logManager;
    private final Logger logger;
    private final WinnerResolver winnerResolver;

    private Raffle activeRaffle;
    private String activeStarterName; // stored so we can log it on end
    private int countdownTaskId = -1;

    public RaffleManager(JavaPlugin plugin, ConfigManager configManager,
                         LogManager logManager, PendingPrizeManager pendingPrizeManager) {
        this.plugin         = plugin;
        this.configManager  = configManager;
        this.logManager     = logManager;
        this.logger         = plugin.getLogger();
        this.winnerResolver = new WinnerResolver(logger, logManager, pendingPrizeManager);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public Optional<Raffle> startRaffle(ItemStack prizeItem, UUID creatorUUID, String starterName) {
        if (activeRaffle != null) return Optional.empty();

        int duration = configManager.getDuration();
        activeRaffle      = new Raffle(prizeItem, creatorUUID, duration);
        activeStarterName = starterName;

        logger.info(String.format("Raffle started by %s — prize: %s | duration: %ds",
                starterName, activeRaffle.getPrizeName(), duration));
        logManager.logRaffleStart(starterName, creatorUUID, activeRaffle.getPrizeName(), duration);

        Bukkit.broadcast(Messages.raffleStartedComponent(activeRaffle.getPrizeName(), starterName));
        scheduleCountdown(duration);
        return Optional.of(activeRaffle);
    }

    public Optional<Raffle> stopRaffle() {
        if (activeRaffle == null) return Optional.empty();
        cancelCountdownTask();
        Raffle stopped = activeRaffle;
        activeRaffle = null;
        logger.info(String.format("Raffle stopped manually — prize: '%s'", stopped.getPrizeName()));
        return Optional.of(stopped);
    }

    // ── Participation ─────────────────────────────────────────────────────────

    public JoinResult joinRaffle(UUID playerUUID) {
        if (activeRaffle == null)                              return JoinResult.NO_ACTIVE_RAFFLE;
        if (activeRaffle.getCreatorUUID().equals(playerUUID)) return JoinResult.CREATOR_CANNOT_JOIN;
        if (activeRaffle.hasParticipant(playerUUID))          return JoinResult.ALREADY_JOINED;
        activeRaffle.addParticipant(playerUUID);
        return JoinResult.SUCCESS;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean isRaffleActive()                { return activeRaffle != null; }
    public Optional<Raffle> getActiveRaffle()      { return Optional.ofNullable(activeRaffle); }
    public Optional<Set<UUID>> getParticipants()   { return getActiveRaffle().map(Raffle::getParticipants); }

    // ── Countdown ─────────────────────────────────────────────────────────────

    private void scheduleCountdown(int durationSeconds) {
        BukkitRunnable task = new BukkitRunnable() {
            int secondsLeft = durationSeconds;

            @Override
            public void run() {
                if (activeRaffle == null) { cancel(); return; }

                if (secondsLeft <= 0) {
                    cancel();
                    Raffle finished = activeRaffle;
                    activeRaffle    = null;
                    countdownTaskId = -1;

                    Bukkit.broadcast(Messages.raffleClosedComponent(
                            finished.getPrizeName(), finished.getParticipantCount()));

                    new BukkitRunnable() {
                        @Override public void run() { Bukkit.broadcast(Messages.raffleDrawing()); }
                    }.runTaskLater(plugin, 20L);

                    new BukkitRunnable() {
                        @Override public void run() { winnerResolver.resolve(finished); }
                    }.runTaskLater(plugin, 3 * 20L);
                    return;
                }

                // Announce at full duration, 20s, 10s, and every second ≤5
                if (secondsLeft == durationSeconds
                        || secondsLeft == 20
                        || secondsLeft == 10
                        || secondsLeft <= 5) {
                    Bukkit.broadcast(Messages.raffleCountdownComponent(
                            activeRaffle.getPrizeName(), secondsLeft));
                }
                secondsLeft--;
            }
        };
        countdownTaskId = task.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    private void cancelCountdownTask() {
        if (countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    public enum JoinResult { SUCCESS, NO_ACTIVE_RAFFLE, ALREADY_JOINED, CREATOR_CANNOT_JOIN }
}
