package com.summitcraft.summitraffle.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads and exposes typed values from config.yml.
 * Call {@link #reload()} to re-read at runtime.
 */
public class ConfigManager {

    private final JavaPlugin plugin;

    private String prefix;
    private int duration;
    private Map<String, Integer> cooldownTiers; // permission suffix → seconds
    private Map<String, String> rawMessages;    // config key → translated string

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        prefix = color(plugin.getConfig().getString("prefix", "&6[SummitRaffle]&r"));

        // Load cooldown tiers
        cooldownTiers = new LinkedHashMap<>();
        ConfigurationSection cd = plugin.getConfig().getConfigurationSection("cooldowns");
        if (cd != null) {
            for (String key : cd.getKeys(false)) {
                cooldownTiers.put(key, cd.getInt(key, 300));
            }
        }
        if (!cooldownTiers.containsKey("default")) {
            cooldownTiers.put("default", 300);
        }

        duration = plugin.getConfig().getInt("duration", 30);

        // Load all messages
        rawMessages = new LinkedHashMap<>();
        ConfigurationSection msgs = plugin.getConfig().getConfigurationSection("messages");
        if (msgs != null) {
            for (String key : msgs.getKeys(false)) {
                rawMessages.put(key, color(msgs.getString(key, "")));
            }
        }
    }

    /** Returns the translated prefix (§-codes applied). */
    public String getPrefix() { return prefix; }

    /** Returns the configured raffle duration in seconds. */
    public int getDuration() { return duration; }

    /**
     * Returns a translated message string, with {prefix} replaced.
     * Returns a fallback if the key is missing.
     */
    public String getMessage(String key) {
        String raw = rawMessages.getOrDefault(key, "&c[Missing message: " + key + "]");
        return raw.replace("{prefix}", prefix);
    }

    /** Returns the full cooldown tier map (permission suffix → seconds). */
    public Map<String, Integer> getCooldownTiers() {
        return cooldownTiers;
    }

    private static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
}
