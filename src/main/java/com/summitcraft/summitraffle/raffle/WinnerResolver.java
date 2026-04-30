package com.summitcraft.summitraffle.raffle;

import com.summitcraft.summitraffle.command.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Resolves the outcome of a finished raffle.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Pick a random winner from the participant set.</li>
 *   <li>Deliver the prize to the winner's inventory, or drop it at their
 *       feet if their inventory is full.</li>
 *   <li>If nobody entered, return the item to the creator (or drop it at
 *       their location if they're offline / have a full inventory).</li>
 *   <li>Broadcast the result server-wide.</li>
 * </ul>
 *
 * <p>This class is intentionally stateless — all inputs come from the
 * finished {@link Raffle} so it can be unit-tested independently.</p>
 */
public class WinnerResolver {

    private static final Random RANDOM = new Random();

    private final Logger logger;

    public WinnerResolver(Logger logger) {
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Resolves the finished raffle: picks a winner (or returns the item to the
     * creator if there are no participants), delivers the prize, and broadcasts.
     *
     * @param raffle the completed raffle
     */
    public void resolve(Raffle raffle) {
        Set<UUID> participants = raffle.getParticipants();

        if (participants.isEmpty()) {
            handleNoParticipants(raffle);
            return;
        }

        UUID winnerUUID = pickRandom(participants);
        handleWinner(raffle, winnerUUID);
    }

    // -------------------------------------------------------------------------
    // No-participants path
    // -------------------------------------------------------------------------

    private void handleNoParticipants(Raffle raffle) {
        ItemStack prize = raffle.getPrizeItem();
        UUID creatorUUID = raffle.getCreatorUUID();

        Bukkit.broadcast(Messages.raffleNoParticipants(raffle.getPrizeName()));
        logger.info(String.format("Raffle ended with no participants — returning '%s' to creator %s",
                raffle.getPrizeName(), creatorUUID));

        Player creator = Bukkit.getPlayer(creatorUUID);
        if (creator != null && creator.isOnline()) {
            deliverItem(creator, prize);
            creator.sendMessage(Messages.prizeReturnedToCreator(raffle.getPrizeName()));
        } else {
            // Creator is offline — the item would be lost; log it clearly
            logger.warning(String.format(
                    "Creator %s is offline — prize '%s' could not be returned. Item dropped at spawn.",
                    creatorUUID, raffle.getPrizeName()));
            // Drop at world spawn as a last resort so nothing is silently deleted
            var spawnWorld = Bukkit.getWorlds().get(0);
            spawnWorld.dropItemNaturally(spawnWorld.getSpawnLocation(), prize);
        }
    }

    // -------------------------------------------------------------------------
    // Winner path
    // -------------------------------------------------------------------------

    private void handleWinner(Raffle raffle, UUID winnerUUID) {
        ItemStack prize = raffle.getPrizeItem();
        Player winner = Bukkit.getPlayer(winnerUUID);

        if (winner != null && winner.isOnline()) {
            deliverItem(winner, prize);
            Bukkit.broadcast(Messages.raffleWinner(winner.getName(), raffle.getPrizeName()));
            logger.info(String.format("Raffle winner: %s — prize: '%s'",
                    winner.getName(), raffle.getPrizeName()));
        } else {
            // Winner logged off between joining and the draw — pick again or drop at spawn
            logger.warning(String.format(
                    "Chosen winner %s is offline — re-drawing from remaining participants.",
                    winnerUUID));

            Set<UUID> remaining = raffle.getParticipants();
            remaining = removeFromSet(remaining, winnerUUID);

            if (remaining.isEmpty()) {
                // Everyone who entered is offline — treat as no-participants
                handleNoParticipants(raffle);
            } else {
                // Recurse with a reduced participant snapshot — safe, bounded by participant count
                UUID nextWinner = pickRandom(remaining);
                handleWinner(raffle, nextWinner);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Prize delivery
    // -------------------------------------------------------------------------

    /**
     * Attempts to add the item to the player's inventory.
     * If the inventory is full, drops it naturally at their feet instead.
     */
    private void deliverItem(Player player, ItemStack prize) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(prize);
        if (!overflow.isEmpty()) {
            // Inventory full — drop every overflow stack at the player's location
            for (ItemStack dropped : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), dropped);
            }
            player.sendMessage(Messages.inventoryFullItemDropped(prize.getType().name()));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID pickRandom(Set<UUID> uuids) {
        List<UUID> list = new ArrayList<>(uuids);
        return list.get(RANDOM.nextInt(list.size()));
    }

    /** Returns a new set with the given UUID removed (does not mutate the original). */
    private Set<UUID> removeFromSet(Set<UUID> original, UUID toRemove) {
        var copy = new java.util.LinkedHashSet<>(original);
        copy.remove(toRemove);
        return copy;
    }
}
