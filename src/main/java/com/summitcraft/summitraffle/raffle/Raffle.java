package com.summitcraft.summitraffle.raffle;

import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a single active raffle session.
 * Immutable metadata (prizeItem, creator, startTime, durationSeconds) is set at construction.
 * Participants are managed via thread-safe methods.
 */
public class Raffle {

    public static final int DURATION_SECONDS = 30;

    private final ItemStack prizeItem;
    private final UUID creatorUUID;
    private final Instant startTime;
    private final Set<UUID> participants;

    public Raffle(ItemStack prizeItem, UUID creatorUUID) {
        if (prizeItem == null || prizeItem.getType().isAir()) {
            throw new IllegalArgumentException("Prize item must not be null or air.");
        }
        if (creatorUUID == null) {
            throw new IllegalArgumentException("Creator UUID must not be null.");
        }

        this.prizeItem = prizeItem.clone(); // defensive copy — caller may modify their stack
        this.creatorUUID = creatorUUID;
        this.startTime = Instant.now();
        this.participants = Collections.synchronizedSet(new LinkedHashSet<>());
    }

    // -------------------------------------------------------------------------
    // Participation
    // -------------------------------------------------------------------------

    public boolean addParticipant(UUID playerUUID) {
        if (playerUUID == null) return false;
        return participants.add(playerUUID);
    }

    public boolean removeParticipant(UUID playerUUID) {
        if (playerUUID == null) return false;
        return participants.remove(playerUUID);
    }

    public boolean hasParticipant(UUID playerUUID) {
        return participants.contains(playerUUID);
    }

    /** Returns an unmodifiable snapshot of current participants. */
    public Set<UUID> getParticipants() {
        synchronized (participants) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(participants));
        }
    }

    public int getParticipantCount() {
        return participants.size();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Returns a defensive copy of the prize item so callers cannot mutate it. */
    public ItemStack getPrizeItem() {
        return prizeItem.clone();
    }

    /** Human-readable prize name for messages (custom name → material name fallback). */
    public String getPrizeName() {
        if (prizeItem.hasItemMeta() && prizeItem.getItemMeta().hasDisplayName()) {
            return prizeItem.getItemMeta().getDisplayName();
        }
        // e.g. DIAMOND_SWORD → "Diamond Sword"
        String raw = prizeItem.getType().name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)))
              .append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public UUID getCreatorUUID() {
        return creatorUUID;
    }

    public Instant getStartTime() {
        return startTime;
    }
}
