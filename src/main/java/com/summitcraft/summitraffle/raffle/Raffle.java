package com.summitcraft.summitraffle.raffle;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a single active raffle session.
 * Immutable metadata (prize, creator, startTime) is set at construction.
 * Participants are managed via thread-safe methods.
 */
public class Raffle {

    private final String prize;
    private final UUID creatorUUID;
    private final Instant startTime;
    private final Set<UUID> participants;

    public Raffle(String prize, UUID creatorUUID) {
        if (prize == null || prize.isBlank()) {
            throw new IllegalArgumentException("Prize must not be null or blank.");
        }
        if (creatorUUID == null) {
            throw new IllegalArgumentException("Creator UUID must not be null.");
        }

        this.prize = prize;
        this.creatorUUID = creatorUUID;
        this.startTime = Instant.now();
        this.participants = Collections.synchronizedSet(new LinkedHashSet<>());
    }

    /**
     * Adds a participant to the raffle.
     *
     * @param playerUUID the UUID of the player to add
     * @return true if the player was added, false if already entered
     */
    public boolean addParticipant(UUID playerUUID) {
        if (playerUUID == null) return false;
        return participants.add(playerUUID);
    }

    /**
     * Removes a participant from the raffle.
     *
     * @param playerUUID the UUID of the player to remove
     * @return true if the player was removed, false if they weren't in the raffle
     */
    public boolean removeParticipant(UUID playerUUID) {
        if (playerUUID == null) return false;
        return participants.remove(playerUUID);
    }

    /**
     * Checks whether a player has already joined this raffle.
     *
     * @param playerUUID the UUID to check
     * @return true if the player is a participant
     */
    public boolean hasParticipant(UUID playerUUID) {
        return participants.contains(playerUUID);
    }

    /**
     * Returns an unmodifiable snapshot of current participants.
     */
    public Set<UUID> getParticipants() {
        synchronized (participants) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(participants));
        }
    }

    public int getParticipantCount() {
        return participants.size();
    }

    public String getPrize() {
        return prize;
    }

    public UUID getCreatorUUID() {
        return creatorUUID;
    }

    public Instant getStartTime() {
        return startTime;
    }
}
