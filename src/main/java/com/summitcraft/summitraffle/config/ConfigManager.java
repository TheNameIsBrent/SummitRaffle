package com.summitcraft.summitraffle.config;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads and exposes typed values from config.yml.
 *
 * <p>Call {@link #reload()} to re-read the file at runtime (e.g. from a
 * future /raffle reload command) without restarting the server.</p>
 */
public class ConfigManager {

    private final JavaPlugin plugin;

    private String prefix;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Re-reads config.yml from disk, applying defaults for any missing keys.
     * Safe to call multiple times.
     */
    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("prefix", "&6[SummitRaffle]&r"));
    }

    /** Returns the translated chat prefix (§-codes already applied). */
    public String getPrefix() {
        return prefix;
    }
}
