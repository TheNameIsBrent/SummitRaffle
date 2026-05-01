package com.summitcraft.summitraffle.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads and exposes typed values from config.yml.
 *
 * <p>Messages are stored <em>raw</em> (not pre-translated) so that
 * {@link com.summitcraft.summitraffle.util.ColorParser} can handle
 * legacy {@code &} codes, inline hex {@code &#RRGGBB}, and
 * {@code <gradient:#HEX1:#HEX2>text</gradient>} tags at render time.</p>
 */
public class ConfigManager {

    private final JavaPlugin plugin;

    private String rawPrefix;      // raw from config — not translated yet
    private int duration;
    private Map<String, Integer> cooldownTiers;
    private Map<String, String> rawMessages;   // raw, un-translated

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        // Store raw — ColorParser will translate at render time
        rawPrefix = plugin.getConfig().getString("prefix", "&6[SummitRaffle]&r");

        // Cooldown tiers
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

        // Messages — raw strings, {prefix} substituted at getMessage() call time
        rawMessages = new LinkedHashMap<>();
        ConfigurationSection msgs = plugin.getConfig().getConfigurationSection("messages");
        if (msgs != null) {
            for (String key : msgs.getKeys(false)) {
                String val = msgs.getString(key, "");
                rawMessages.put(key, val != null ? val : "");
            }
        }
    }

    /**
     * Returns the raw prefix string (un-translated, may contain &/hex/gradient codes).
     */
    public String getRawPrefix() { return rawPrefix; }

    /** Returns the configured raffle duration in seconds. */
    public int getDuration() { return duration; }

    /**
     * Returns a raw message string with {prefix} substituted.
     * Callers are responsible for passing this through
     * {@link com.summitcraft.summitraffle.util.ColorParser#parse}.
     */
    public String getRawMessage(String key) {
        String raw = rawMessages.getOrDefault(key, "&c[Missing message: " + key + "]");
        return raw.replace("{prefix}", rawPrefix);
    }

    /** Returns the full cooldown tier map (permission suffix → seconds). */
    public Map<String, Integer> getCooldownTiers() { return cooldownTiers; }
}
