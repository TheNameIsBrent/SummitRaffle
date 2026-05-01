package com.summitcraft.summitraffle.cooldown;

import com.summitcraft.summitraffle.config.ConfigManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player raffle start cooldowns.
 *
 * <p>Permission tiers are resolved at check-time so changing config values
 * and reloading takes effect immediately.</p>
 *
 * <p>Cooldowns are in-memory only — a server restart clears them, which is
 * acceptable UX for a raffle plugin.</p>
 */
public class CooldownManager {

    private final ConfigManager configManager;
    private final Map<UUID, Long> lastStartTime = new HashMap<>();

    public CooldownManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Returns remaining cooldown seconds for a player, or 0 if ready.
     */
    public int getRemainingCooldown(Player player) {
        Long last = lastStartTime.get(player.getUniqueId());
        if (last == null) return 0;
        int cooldownSeconds = resolvePlayerCooldown(player);
        long elapsed = (System.currentTimeMillis() - last) / 1000L;
        return (int) Math.max(0, cooldownSeconds - elapsed);
    }

    /** Returns true if the player may start a raffle right now. */
    public boolean isReady(Player player) {
        return getRemainingCooldown(player) == 0;
    }

    /** Records that the player just started a raffle. */
    public void recordStart(UUID playerUUID) {
        lastStartTime.put(playerUUID, System.currentTimeMillis());
    }

    /** Admin override — clears a player's cooldown. */
    public void clearCooldown(UUID playerUUID) {
        lastStartTime.remove(playerUUID);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Finds the lowest cooldown the player qualifies for across all configured
     * tiers. A player with raffle.mvp (60s) always beats raffle.vip (180s).
     */
    private int resolvePlayerCooldown(Player player) {
        Map<String, Integer> tiers = configManager.getCooldownTiers();
        int lowest = tiers.getOrDefault("default", 300);
        for (Map.Entry<String, Integer> entry : tiers.entrySet()) {
            if (entry.getKey().equals("default")) continue;
            if (player.hasPermission("raffle.cooldown." + entry.getKey())) {
                lowest = Math.min(lowest, entry.getValue());
            }
        }
        return lowest;
    }
}
