package com.summitcraft.summitraffle.prize;

import com.summitcraft.summitraffle.command.Messages;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Persists unclaimed prizes for offline players and delivers them on next login.
 *
 * <h3>Storage</h3>
 * <p>Items are serialized to binary via {@link ItemStack#serializeAsBytes()} and
 * stored as Base64 strings in {@code pending_prizes.yml} inside the plugin's data
 * folder. This means the file survives server restarts and plugin reloads, and the
 * full NBT of each item is preserved.</p>
 *
 * <h3>Multiple pending prizes</h3>
 * <p>A player can accumulate more than one pending prize (e.g. they won two raffles
 * while offline). All are delivered in order on next login.</p>
 *
 * <h3>Full inventory on delivery</h3>
 * <p>If the player's inventory is still full when they log in, the item drops at
 * their feet. It is <em>not</em> re-queued — the item is physically in the world
 * and the player is informed immediately.</p>
 */
public class PendingPrizeManager implements Listener {

    private static final String FILE_NAME = "pending_prizes.yml";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File dataFile;
    private YamlConfiguration data;

    public PendingPrizeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataFile = new File(plugin.getDataFolder(), FILE_NAME);
        load();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Queues an item for a player who is offline (or whose inventory is full).
     * The item is immediately persisted to disk.
     *
     * @param playerUUID the recipient's UUID
     * @param item       the item to queue (full NBT preserved)
     */
    public void queuePrize(UUID playerUUID, ItemStack item) {
        String key = playerUUID.toString();
        List<String> existing = data.getStringList(key);
        existing.add(encode(item));
        data.set(key, existing);
        save();
        logger.info(String.format("Queued pending prize '%s' for offline player %s",
                item.getType().name(), playerUUID));
    }

    /** Returns true if the player has at least one pending prize. */
    public boolean hasPending(UUID playerUUID) {
        return !data.getStringList(playerUUID.toString()).isEmpty();
    }

    // ── PlayerJoinEvent delivery ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        List<String> encoded = data.getStringList(uuid.toString());
        if (encoded.isEmpty()) return;

        List<String> failed = new ArrayList<>();

        for (String b64 : encoded) {
            ItemStack item = decode(b64);
            if (item == null) {
                logger.warning("Could not decode pending prize for " + uuid + " — skipping corrupt entry.");
                continue;
            }

            // Attempt to give via addItem; overflow drops at feet
            var overflow = player.getInventory().addItem(item);
            if (!overflow.isEmpty()) {
                for (ItemStack dropped : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), dropped);
                }
                player.sendMessage(Messages.pendingPrizeFull(item.getType().name()));
            } else {
                player.sendMessage(Messages.pendingPrizeReceived(friendlyName(item)));
            }
        }

        // Clear delivered prizes from disk
        data.set(uuid.toString(), failed.isEmpty() ? null : failed);
        save();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) {
            data = new YamlConfiguration();
            return;
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            logger.severe("Failed to save pending_prizes.yml: " + e.getMessage());
        }
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    private static String encode(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    private static ItemStack decode(String b64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(b64);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static String friendlyName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String raw = item.getType().name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)))
              .append(word.substring(1).toLowerCase());
        }
        return item.getAmount() > 1 ? item.getAmount() + "x " + sb : sb.toString();
    }
}
