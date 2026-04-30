package com.summitcraft.summitraffle.raffle;

import com.summitcraft.summitraffle.command.Messages;
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
 *
 * <p>Owns the Bukkit countdown task — cancels it cleanly on {@link #stopRaffle()}
 * or server shutdown so no orphaned tasks linger.</p>
 *
 * <p>All server-wide broadcasts use the Adventure API ({@code Bukkit.broadcast(Component)})
 * so click/hover events are preserved. Per-player feedback in commands uses legacy
 * §-codes for simplicity.</p>
 */
public class RaffleManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final WinnerResolver winnerResolver;

    private Raffle activeRaffle;
    private int countdownTaskId = -1;

    public RaffleManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.winnerResolver = new WinnerResolver(logger);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts a new raffle with the given item as the prize.
     * Schedules a 30-second countdown that ends the raffle automatically.
     *
     * @param prizeItem   the item to raffle off (taken from the starter's hand by the caller)
     * @param creatorUUID UUID of the player who started the raffle
     * @return the new {@link Raffle}, or empty if one is already active
     */
    public Optional<Raffle> startRaffle(ItemStack prizeItem, UUID creatorUUID, String starterName) {
        if (activeRaffle != null) {
            return Optional.empty();
        }

        activeRaffle = new Raffle(prizeItem, creatorUUID);
        logger.info(String.format("Raffle started by %s — prize: %s",
                starterName, activeRaffle.getPrizeName()));

        // Rich opening broadcast with clickable join button
        Bukkit.broadcast(Messages.raffleStartedComponent(activeRaffle.getPrizeName(), starterName));

        scheduleCountdown();
        return Optional.of(activeRaffle);
    }

    /**
     * Stops the active raffle immediately (manual cancel or end-of-countdown).
     *
     * @return the stopped {@link Raffle}, or empty if none was active
     */
    public Optional<Raffle> stopRaffle() {
        if (activeRaffle == null) {
            return Optional.empty();
        }

        cancelCountdownTask();

        Raffle stopped = activeRaffle;
        activeRaffle = null;
        logger.info(String.format("Raffle ended — prize: '%s' | participants: %d",
                stopped.getPrizeName(), stopped.getParticipantCount()));
        return Optional.of(stopped);
    }

    // -------------------------------------------------------------------------
    // Participation
    // -------------------------------------------------------------------------

    public JoinResult joinRaffle(UUID playerUUID) {
        if (activeRaffle == null)                              return JoinResult.NO_ACTIVE_RAFFLE;
        if (activeRaffle.getCreatorUUID().equals(playerUUID)) return JoinResult.CREATOR_CANNOT_JOIN;
        if (activeRaffle.hasParticipant(playerUUID))          return JoinResult.ALREADY_JOINED;
        activeRaffle.addParticipant(playerUUID);
        return JoinResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public boolean isRaffleActive() {
        return activeRaffle != null;
    }

    public Optional<Raffle> getActiveRaffle() {
        return Optional.ofNullable(activeRaffle);
    }

    public Optional<Set<UUID>> getParticipants() {
        return getActiveRaffle().map(Raffle::getParticipants);
    }

    // -------------------------------------------------------------------------
    // Countdown
    // -------------------------------------------------------------------------

    private void scheduleCountdown() {
        BukkitRunnable task = new BukkitRunnable() {

            int secondsLeft = Raffle.DURATION_SECONDS;

            @Override
            public void run() {
                // Raffle was cancelled externally (e.g. server shutdown)
                if (activeRaffle == null) {
                    cancel();
                    return;
                }

                if (secondsLeft <= 0) {
                    cancel();
                    Raffle finished = activeRaffle;
                    activeRaffle = null;
                    countdownTaskId = -1;
                    logger.info(String.format("Raffle countdown finished — prize: '%s' | participants: %d",
                            finished.getPrizeName(), finished.getParticipantCount()));
                    Bukkit.broadcast(Messages.raffleClosedComponent(
                            finished.getPrizeName(), finished.getParticipantCount()));
                    winnerResolver.resolve(finished);
                    return;
                }

                // Announce at 30s, 20s, 10s, and every second from 5 down to 1
                if (secondsLeft == Raffle.DURATION_SECONDS
                        || secondsLeft == 20
                        || secondsLeft == 10
                        || secondsLeft <= 5) {
                    Bukkit.broadcast(Messages.raffleCountdownComponent(
                            activeRaffle.getPrizeName(), secondsLeft));
                }

                secondsLeft--;
            }
        };

        // Run every 20 ticks (1 second), starting immediately
        countdownTaskId = task.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    private void cancelCountdownTask() {
        if (countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    public enum JoinResult {
        SUCCESS,
        NO_ACTIVE_RAFFLE,
        ALREADY_JOINED,
        CREATOR_CANNOT_JOIN
    }
}
