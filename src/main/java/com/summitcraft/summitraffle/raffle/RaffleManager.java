package com.summitcraft.summitraffle.raffle;

import com.summitcraft.summitraffle.command.Messages;
import com.summitcraft.summitraffle.config.ConfigManager;
import com.summitcraft.summitraffle.logging.LogManager;
import com.summitcraft.summitraffle.prize.PendingPrizeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of raffles.
 *
 * <p>Edge cases handled here:</p>
 * <ul>
 *   <li>Only one active raffle at a time — enforced by null-check on {@link #activeRaffle}.</li>
 *   <li>Duplicate joins — guarded in {@link #joinRaffle} before {@link Raffle#addParticipant}.</li>
 *   <li>Server shutdown/reload — {@link #cancelAndReturn} queues the prize to the creator
 *       via {@link PendingPrizeManager} so nothing is lost.</li>
 *   <li>Admin force-stop — {@link #forceStop} cancels the raffle and returns the item.</li>
 * </ul>
 */
public class RaffleManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final LogManager logManager;
    private final Logger logger;
    private final PendingPrizeManager pendingPrizeManager;
    private final WinnerResolver winnerResolver;

    private Raffle activeRaffle;
    private String activeStarterName;
    private int countdownTaskId = -1;

    public RaffleManager(JavaPlugin plugin, ConfigManager configManager,
                         LogManager logManager, PendingPrizeManager pendingPrizeManager) {
        this.plugin               = plugin;
        this.configManager        = configManager;
        this.logManager           = logManager;
        this.logger               = plugin.getLogger();
        this.pendingPrizeManager  = pendingPrizeManager;
        this.winnerResolver       = new WinnerResolver(logger, logManager, pendingPrizeManager);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Starts a new raffle. Returns empty if one is already active.
     */
    public Optional<Raffle> startRaffle(ItemStack prizeItem, UUID creatorUUID, String starterName) {
        if (activeRaffle != null) return Optional.empty();

        int duration      = configManager.getDuration();
        activeRaffle      = new Raffle(prizeItem, creatorUUID, duration);
        activeStarterName = starterName;

        logger.info(String.format("[START] %s started a raffle — prize: %s | duration: %ds",
                starterName, activeRaffle.getPrizeName(), duration));
        logManager.logRaffleStart(starterName, creatorUUID, activeRaffle.getPrizeName(), duration);

        Bukkit.broadcast(Messages.raffleStartedComponent(activeRaffle.getPrizeName(), starterName));
        scheduleCountdown(duration);
        return Optional.of(activeRaffle);
    }

    /**
     * Admin force-stop: cancels the active raffle, returns the item to the creator,
     * and broadcasts a cancellation message.
     *
     * @param cancellerName name of the admin who cancelled (for broadcast)
     * @return the cancelled Raffle, or empty if none was active
     */
    public Optional<Raffle> forceStop(String cancellerName) {
        if (activeRaffle == null) return Optional.empty();

        cancelCountdownTask();
        Raffle cancelled = activeRaffle;
        activeRaffle     = null;

        logger.info(String.format("[FORCE STOP] %s cancelled the raffle — prize: '%s' being returned to creator.",
                cancellerName, cancelled.getPrizeName()));

        // Return prize to creator
        returnToCreator(cancelled);

        Bukkit.broadcast(Messages.raffleCancelled(cancelled.getPrizeName(), cancellerName));
        return Optional.of(cancelled);
    }

    /**
     * Called from {@code onDisable} during server shutdown or reload.
     * Cancels the active raffle silently and queues the prize to the creator.
     */
    public void cancelAndReturn() {
        if (activeRaffle == null) return;

        cancelCountdownTask();
        Raffle cancelled = activeRaffle;
        activeRaffle     = null;

        logger.warning(String.format(
                "[SHUTDOWN] Server stopped with active raffle. Prize '%s' queued for creator %s.",
                cancelled.getPrizeName(), cancelled.getCreatorUUID()));

        // Creator gets item back on next login regardless of online status
        pendingPrizeManager.queuePrize(cancelled.getCreatorUUID(), cancelled.getPrizeItem());
    }

    /**
     * @deprecated Use {@link #forceStop} for admin cancellation, or {@link #cancelAndReturn}
     *             for shutdown. Kept for internal use only.
     */
    @Deprecated
    public Optional<Raffle> stopRaffle() {
        return forceStop("SERVER");
    }

    // ── Participation ─────────────────────────────────────────────────────────

    /**
     * Attempts to add a player to the active raffle.
     * Guards against: no active raffle, creator joining own raffle, duplicate joins.
     */
    public JoinResult joinRaffle(UUID playerUUID) {
        if (activeRaffle == null)                              return JoinResult.NO_ACTIVE_RAFFLE;
        if (activeRaffle.getCreatorUUID().equals(playerUUID)) return JoinResult.CREATOR_CANNOT_JOIN;
        if (activeRaffle.hasParticipant(playerUUID))          return JoinResult.ALREADY_JOINED;
        activeRaffle.addParticipant(playerUUID);
        return JoinResult.SUCCESS;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean isRaffleActive()              { return activeRaffle != null; }
    public Optional<Raffle> getActiveRaffle()    { return Optional.ofNullable(activeRaffle); }
    public Optional<Set<UUID>> getParticipants() { return getActiveRaffle().map(Raffle::getParticipants); }

    // ── Countdown ─────────────────────────────────────────────────────────────

    private void scheduleCountdown(int durationSeconds) {
        BukkitRunnable task = new BukkitRunnable() {
            int secondsLeft = durationSeconds;

            @Override
            public void run() {
                // Raffle was cancelled externally (force-stop or shutdown)
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

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Attempts to give the prize to the creator if online; otherwise queues it.
     */
    private void returnToCreator(Raffle raffle) {
        UUID creatorUUID = raffle.getCreatorUUID();
        ItemStack prize  = raffle.getPrizeItem();
        Player creator   = Bukkit.getPlayer(creatorUUID);

        if (creator != null && creator.isOnline()) {
            var overflow = creator.getInventory().addItem(prize);
            if (!overflow.isEmpty()) {
                overflow.values().forEach(i -> creator.getWorld().dropItemNaturally(creator.getLocation(), i));
                creator.sendMessage(Messages.inventoryFullItemDropped(prize.getType().name()));
            } else {
                creator.sendMessage(Messages.prizeReturnedToCreator(raffle.getPrizeName()));
            }
        } else {
            pendingPrizeManager.queuePrize(creatorUUID, prize);
        }
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    public enum JoinResult { SUCCESS, NO_ACTIVE_RAFFLE, ALREADY_JOINED, CREATOR_CANNOT_JOIN }
}
