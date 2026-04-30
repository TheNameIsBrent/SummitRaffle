package com.summitcraft.summitraffle.raffle;

import com.summitcraft.summitraffle.command.Messages;
import com.summitcraft.summitraffle.logging.LogManager;
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
 * Resolves the outcome of a finished raffle: picks a winner, delivers the
 * prize, logs the result, and handles offline recipients via
 * {@link PendingPrizeManager}.
 */
public class WinnerResolver {

    private static final Random RANDOM = new Random();

    private final Logger logger;
    private final LogManager logManager;
    private final PendingPrizeManager pendingPrizeManager;

    public WinnerResolver(Logger logger, LogManager logManager,
                          PendingPrizeManager pendingPrizeManager) {
        this.logger              = logger;
        this.logManager          = logManager;
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
        logManager.logRaffleEnd(raffle.getPrizeName(), null, null, raffle.getParticipants(), false);
        logger.info("Raffle ended with no participants — prize: " + raffle.getPrizeName());
        deliverOrQueue(raffle.getCreatorUUID(), raffle.getPrizeItem(), raffle.getPrizeName(), true);
    }

    // ── Winner path ───────────────────────────────────────────────────────────

    private void handleWinner(Raffle raffle, Set<UUID> pool) {
        if (pool.isEmpty()) {
            handleNoParticipants(raffle);
            return;
        }

        UUID winnerUUID = pickRandom(pool);
        Player winner   = Bukkit.getPlayer(winnerUUID);
        boolean online  = winner != null && winner.isOnline();
        String name     = online ? winner.getName() : winnerUUID.toString();

        logManager.logRaffleEnd(raffle.getPrizeName(), name, winnerUUID,
                raffle.getParticipants(), online);

        if (online) {
            Bukkit.broadcast(Messages.raffleWinner(winner.getName(), raffle.getPrizeName()));
            logger.info("Raffle winner: " + winner.getName() + " — prize: " + raffle.getPrizeName());
            deliverOrQueue(winnerUUID, raffle.getPrizeItem(), raffle.getPrizeName(), false);
        } else {
            logger.warning("Winner " + winnerUUID + " is offline — prize queued.");
            pendingPrizeManager.queuePrize(winnerUUID, raffle.getPrizeItem());
            Bukkit.broadcast(Messages.raffleWinner(winnerUUID.toString(), raffle.getPrizeName()));
        }
    }

    // ── Delivery ──────────────────────────────────────────────────────────────

    private void deliverOrQueue(UUID recipientUUID, ItemStack prize,
                                String prizeName, boolean isCreator) {
        Player recipient = Bukkit.getPlayer(recipientUUID);

        if (recipient == null || !recipient.isOnline()) {
            pendingPrizeManager.queuePrize(recipientUUID, prize);
            logger.info("Recipient " + recipientUUID + " offline — prize queued.");
            return;
        }

        if (isCreator) recipient.sendMessage(Messages.prizeReturnedToCreator(prizeName));

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
