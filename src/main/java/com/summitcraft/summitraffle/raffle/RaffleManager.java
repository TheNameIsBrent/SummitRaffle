package com.summitcraft.summitraffle.raffle;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of raffles on the server.
 * Enforces the rule that only ONE raffle may be active at any time.
 *
 * <p>This class is the single source of truth for raffle state and is
 * intentionally decoupled from commands, events, and plugin internals
 * so it can be tested and extended independently.</p>
 */
public class RaffleManager {

    private final Logger logger;
    private Raffle activeRaffle;

    public RaffleManager(Logger logger) {
        this.logger = logger;
        this.activeRaffle = null;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts a new raffle if none is currently active.
     *
     * @param prize       the prize description
     * @param creatorUUID the UUID of the player or console starting the raffle
     * @return the newly created {@link Raffle}, or empty if one is already running
     */
    public Optional<Raffle> startRaffle(String prize, UUID creatorUUID) {
        if (activeRaffle != null) {
            logger.warning("Attempted to start a raffle while one is already active.");
            return Optional.empty();
        }

        activeRaffle = new Raffle(prize, creatorUUID);
        logger.info(String.format("Raffle started by %s for prize: %s", creatorUUID, prize));
        return Optional.of(activeRaffle);
    }

    /**
     * Stops the active raffle and returns it for winner resolution.
     *
     * @return the stopped {@link Raffle}, or empty if no raffle was running
     */
    public Optional<Raffle> stopRaffle() {
        if (activeRaffle == null) {
            logger.warning("Attempted to stop a raffle, but none is active.");
            return Optional.empty();
        }

        Raffle stopped = activeRaffle;
        activeRaffle = null;
        logger.info(String.format(
                "Raffle stopped. Prize: '%s' | Participants: %d",
                stopped.getPrize(),
                stopped.getParticipantCount()
        ));
        return Optional.of(stopped);
    }

    // -------------------------------------------------------------------------
    // Participation
    // -------------------------------------------------------------------------

    /**
     * Attempts to add a player to the active raffle.
     *
     * @param playerUUID the UUID of the player joining
     * @return the result of the join attempt
     */
    public JoinResult joinRaffle(UUID playerUUID) {
        if (activeRaffle == null) {
            return JoinResult.NO_ACTIVE_RAFFLE;
        }
        if (activeRaffle.hasParticipant(playerUUID)) {
            return JoinResult.ALREADY_JOINED;
        }
        activeRaffle.addParticipant(playerUUID);
        return JoinResult.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns whether a raffle is currently active.
     */
    public boolean isRaffleActive() {
        return activeRaffle != null;
    }

    /**
     * Returns the active raffle, if one exists.
     */
    public Optional<Raffle> getActiveRaffle() {
        return Optional.ofNullable(activeRaffle);
    }

    /**
     * Returns an unmodifiable snapshot of current participants, or empty if no raffle is running.
     */
    public Optional<Set<UUID>> getParticipants() {
        return getActiveRaffle().map(Raffle::getParticipants);
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /**
     * Represents the outcome of a player attempting to join a raffle.
     */
    public enum JoinResult {
        /** Player successfully joined the raffle. */
        SUCCESS,
        /** No raffle is currently running. */
        NO_ACTIVE_RAFFLE,
        /** Player has already joined this raffle. */
        ALREADY_JOINED
    }
}
