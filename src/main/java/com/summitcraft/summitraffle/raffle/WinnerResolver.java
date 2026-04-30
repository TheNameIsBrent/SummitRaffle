package com.summitcraft.summitraffle.raffle;

import com.summitcraft.summitraffle.command.Messages;
import com.summitcraft.summitraffle.prize.PendingPrizeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Resolves the outcome of a finished raffle.
 *
 * <p>Offline players (winner or creator) have their item queued in
 * {@link PendingPrizeManager} so nothing is lost — it is delivered on next login.</p>
 */
public class WinnerResolver {

    private static final Random RANDOM = new Random();

    private final Logger logger;
    private final PendingPrizeManager pendingPrizeManager;

    public WinnerResolver(Logger logger, PendingPrizeManager pendingPrizeManager) {
        this.logger = logger;
        this.pendingPrizeManager = pendingPrizeManager;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void resolve(Raffle raffle) {
        Set<UUID> participants = raffle.getParticipants();
        if (participants.isEmpty()) {
            handleNoParticipants(raffle);
        } else {
            handleWinner(raffle, new LinkedHashSet<>(participants));
        }
    }

    // ── No-participants path ──────────────────────────────────────────────────

    private void handleNoParticipants(Raffle raffle) {
        Bukkit.broadcast(Messages.raffleNoParticipants(raffle.getPrizeName()));
        logger.info("Raffle ended with no participants — prize: " + raffle.getPrizeName());
        deliverOrQueue(raffle.getCreatorUUID(), raffle.getPrizeItem(), raffle.getPrizeName(), true);
    }

    // ── Winner path ───────────────────────────────────────────────────────────

    private void handleWinner(Raffle raffle, Set<UUID> pool) {
        if (pool.isEmpty()) {
            // All entrants went offline — fall back to returning to creator
            handleNoParticipants(raffle);
            return;
        }

        UUID winnerUUID = pickRandom(pool);
        Player winner = Bukkit.getPlayer(winnerUUID);

        if (winner != null && winner.isOnline()) {
            Bukkit.broadcast(Messages.raffleWinner(winner.getName(), raffle.getPrizeName()));
            logger.info("Raffle winner: " + winner.getName() + " — prize: " + raffle.getPrizeName());
            deliverOrQueue(winnerUUID, raffle.getPrizeItem(), raffle.getPrizeName(), false);
        } else {
            // Winner offline — re-draw from remaining pool
            logger.warning("Chosen winner " + winnerUUID + " is offline — re-drawing.");
            pool.remove(winnerUUID);
            // Queue for the offline player anyway (they still won; we just re-draw for broadcast)
            pendingPrizeManager.queuePrize(winnerUUID, raffle.getPrizeItem());
            Bukkit.broadcast(Messages.raffleWinner(winnerUUID.toString(), raffle.getPrizeName()));
            handleWinner(raffle, pool);
        }
    }

    // ── Delivery ──────────────────────────────────────────────────────────────

    /**
     * Tries to give the item to an online player. If they're offline or their
     * inventory is full, queues it in {@link PendingPrizeManager}.
     *
     * @param isCreator true when returning to creator (no-participants case)
     */
    private void deliverOrQueue(UUID recipientUUID, ItemStack prize, String prizeName, boolean isCreator) {
        Player recipient = Bukkit.getPlayer(recipientUUID);

        if (recipient == null || !recipient.isOnline()) {
            // Offline — persist for next login
            pendingPrizeManager.queuePrize(recipientUUID, prize);
            logger.info("Recipient " + recipientUUID + " is offline — prize queued for next login.");
            return;
        }

        if (isCreator) {
            recipient.sendMessage(Messages.prizeReturnedToCreator(prizeName));
        }

        var overflow = recipient.getInventory().addItem(prize);
        if (!overflow.isEmpty()) {
            for (ItemStack dropped : overflow.values()) {
                recipient.getWorld().dropItemNaturally(recipient.getLocation(), dropped);
            }
            recipient.sendMessage(Messages.inventoryFullItemDropped(prize.getType().name()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID pickRandom(Set<UUID> uuids) {
        List<UUID> list = new ArrayList<>(uuids);
        return list.get(RANDOM.nextInt(list.size()));
    }
}
