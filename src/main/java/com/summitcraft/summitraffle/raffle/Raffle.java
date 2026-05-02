package com.summitcraft.summitraffle.raffle;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a single active raffle session.
 *
 * <p>Prize names are exposed two ways:</p>
 * <ul>
 *   <li>{@link #getPrizeName()} — plain text (no colour codes), for logs,
 *       measurements, and anywhere a String is needed.</li>
 *   <li>{@link #getPrizeNameComponent()} — full Adventure Component with the
 *       item's original colours intact, for chat broadcasts.</li>
 * </ul>
 */
public class Raffle {

    private final ItemStack prizeItem;
    private final UUID creatorUUID;
    private final Instant startTime;
    private final int durationSeconds;
    private final Set<UUID> participants;

    public Raffle(ItemStack prizeItem, UUID creatorUUID, int durationSeconds) {
        if (prizeItem == null || prizeItem.getType().isAir()) {
            throw new IllegalArgumentException("Prize item must not be null or air.");
        }
        if (creatorUUID == null) {
            throw new IllegalArgumentException("Creator UUID must not be null.");
        }
        this.prizeItem       = ItemStack.deserializeBytes(prizeItem.serializeAsBytes());
        this.creatorUUID     = creatorUUID;
        this.startTime       = Instant.now();
        this.durationSeconds = durationSeconds;
        this.participants    = Collections.synchronizedSet(new LinkedHashSet<>());
    }

    // ── Participation ─────────────────────────────────────────────────────────

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

    public Set<UUID> getParticipants() {
        synchronized (participants) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(participants));
        }
    }

    public int getParticipantCount() { return participants.size(); }

    // ── Prize accessors ───────────────────────────────────────────────────────

    /** Returns a deep copy of the prize item. */
    public ItemStack getPrizeItem() {
        return ItemStack.deserializeBytes(prizeItem.serializeAsBytes());
    }

    /**
     * Plain-text prize name — no colour codes, safe for logs and String contexts.
     * Example: {@code "64x Voting Crate Key"}
     */
    public String getPrizeName() {
        String base = PlainTextComponentSerializer.plainText()
                .serialize(baseNameComponent());
        return prizeItem.getAmount() > 1
                ? prizeItem.getAmount() + "x " + base
                : base;
    }

    /**
     * Full Adventure Component prize name — preserves all colours the item had.
     * Example: a Component that renders as {@code "64x §x§4§1§d§c§3§cVoting Crate Key"}
     * correctly in gold/aqua/etc.
     */
    public Component getPrizeNameComponent() {
        Component base = baseNameComponent();
        if (prizeItem.getAmount() > 1) {
            return Component.text(prizeItem.getAmount() + "x ").append(base);
        }
        return base;
    }

    /**
     * Returns the base name Component (no quantity prefix).
     * Uses the item's Adventure display name if set, otherwise formats the
     * material name as a plain white Component.
     */
    private Component baseNameComponent() {
        ItemMeta meta = prizeItem.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            // displayName() returns the Adventure Component with all colours intact
            return meta.displayName();
        }
        // Fall back to formatted material name — plain text, no colours
        String raw = prizeItem.getType().name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)))
              .append(word.substring(1).toLowerCase());
        }
        return Component.text(sb.toString());
    }

    // ── Other accessors ───────────────────────────────────────────────────────

    public UUID getCreatorUUID()    { return creatorUUID; }
    public Instant getStartTime()   { return startTime; }
    public int getDurationSeconds() { return durationSeconds; }
}
