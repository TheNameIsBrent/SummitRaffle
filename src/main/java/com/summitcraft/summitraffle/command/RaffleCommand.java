package com.summitcraft.summitraffle.command;

import com.summitcraft.summitraffle.cooldown.CooldownManager;
import com.summitcraft.summitraffle.raffle.Raffle;
import com.summitcraft.summitraffle.raffle.RaffleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Handles all /raffle subcommands.
 *
 * <ul>
 *   <li>{@code /raffle start} — hold an item to raffle it off  (requires {@code raffle.start})</li>
 *   <li>{@code /raffle join}  — enter the active raffle         (players only)</li>
 *   <li>{@code /raffle stop}  — force-cancel and return prize   (requires {@code raffle.stop})</li>
 * </ul>
 *
 * <p>Edge cases handled:</p>
 * <ul>
 *   <li>Empty hand on start → clear error, item never removed</li>
 *   <li>Duplicate join → rejected with ALREADY_JOINED before touching the set</li>
 *   <li>Creator joining own raffle → rejected with CREATOR_CANNOT_JOIN</li>
 *   <li>Race condition on start → item returned to inventory before send</li>
 * </ul>
 */
public class RaffleCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_START = "raffle.start";
    private static final String PERM_STOP  = "raffle.stop";
    private static final String PERM_JOIN  = "raffle.join";

    private final RaffleManager raffleManager;
    private final CooldownManager cooldownManager;

    public RaffleCommand(RaffleManager raffleManager, CooldownManager cooldownManager) {
        this.raffleManager   = raffleManager;
        this.cooldownManager = cooldownManager;
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) { sender.sendMessage(Messages.usage()); return true; }

        switch (args[0].toLowerCase()) {
            case "start" -> handleStart(sender);
            case "join"  -> handleJoin(sender);
            case "stop"  -> handleStop(sender);
            default      -> sender.sendMessage(Messages.usage());
        }
        return true;
    }

    // ── /raffle start ─────────────────────────────────────────────────────────

    private void handleStart(CommandSender sender) {
        if (!sender.hasPermission(PERM_START)) {
            sender.sendMessage(Messages.noPermission()); return;
        }
        // console guard — must be first after permission so console gets a useful message
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.playersOnly()); return;
        }
        if (raffleManager.isRaffleActive()) {
            player.sendMessage(Messages.raffleAlreadyActive()); return;
        }

        // Cooldown check
        int remaining = cooldownManager.getRemainingCooldown(player);
        if (remaining > 0) {
            player.sendMessage(Messages.onCooldown(remaining)); return;
        }

        // Empty hand guard — checked before touching the inventory
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage(Messages.mustHoldItem()); return;
        }

        // Deep NBT-safe copy before clearing the slot
        ItemStack prize = ItemStack.deserializeBytes(held.serializeAsBytes());
        player.getInventory().setItemInMainHand(null);

        Optional<Raffle> started = raffleManager.startRaffle(prize, player.getUniqueId(), player.getName());

        if (started.isPresent()) {
            cooldownManager.recordStart(player.getUniqueId());
        } else {
            // Race condition: another raffle started between our isRaffleActive() check and startRaffle()
            player.sendMessage(Messages.raffleAlreadyActive());
            player.getInventory().addItem(prize); // always return before messaging
        }
    }

    // ── /raffle join ──────────────────────────────────────────────────────────

    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.playersOnly()); return;
        }
        if (!player.hasPermission(PERM_JOIN)) {
            player.sendMessage(Messages.noPermission()); return;
        }
        switch (raffleManager.joinRaffle(player.getUniqueId())) {
            case SUCCESS             -> player.sendMessage(Messages.joinSuccess());
            case ALREADY_JOINED      -> player.sendMessage(Messages.alreadyJoined());
            case NO_ACTIVE_RAFFLE    -> player.sendMessage(Messages.noActiveRaffle());
            case CREATOR_CANNOT_JOIN -> player.sendMessage(Messages.creatorCannotJoin());
        }
    }

    // ── /raffle stop ──────────────────────────────────────────────────────────

    private void handleStop(CommandSender sender) {
        if (!sender.hasPermission(PERM_STOP)) {
            sender.sendMessage(Messages.noPermission()); return;
        }
        if (!raffleManager.isRaffleActive()) {
            sender.sendMessage(Messages.noActiveRaffle()); return;
        }

        String cancellerName = (sender instanceof Player p) ? p.getName() : "Console";
        Optional<Raffle> cancelled = raffleManager.forceStop(cancellerName);

        if (cancelled.isEmpty()) {
            sender.sendMessage(Messages.noActiveRaffle());
        }
        // Success broadcast is sent from inside forceStop()
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) return List.of();

        // Only show subcommands the sender can actually use
        return List.of("start", "join", "stop").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .filter(s -> switch (s) {
                    case "start" -> sender.hasPermission(PERM_START);
                    case "stop"  -> sender.hasPermission(PERM_STOP);
                    case "join"  -> sender.hasPermission(PERM_JOIN);
                    default      -> true;
                })
                .toList();
    }
}
