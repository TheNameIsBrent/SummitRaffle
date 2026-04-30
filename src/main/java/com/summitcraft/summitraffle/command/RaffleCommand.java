package com.summitcraft.summitraffle.command;

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
 *   <li>{@code /raffle start} — hold an item; it becomes the prize (requires {@code raffle.start})</li>
 *   <li>{@code /raffle join}  — join the active raffle (players only)</li>
 * </ul>
 */
public class RaffleCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_START = "raffle.start";

    private final RaffleManager raffleManager;

    public RaffleCommand(RaffleManager raffleManager) {
        this.raffleManager = raffleManager;
    }

    // -------------------------------------------------------------------------
    // Dispatch
    // -------------------------------------------------------------------------

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            sender.sendMessage(Messages.USAGE);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> handleStart(sender);
            case "join"  -> handleJoin(sender);
            default      -> sender.sendMessage(Messages.USAGE);
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // /raffle start
    // -------------------------------------------------------------------------

    private void handleStart(CommandSender sender) {
        if (!sender.hasPermission(PERM_START)) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return;
        }

        // Must be a player — console cannot hold items
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYERS_ONLY);
            return;
        }

        // Block if a raffle is already running
        if (raffleManager.isRaffleActive()) {
            player.sendMessage(Messages.RAFFLE_ALREADY_ACTIVE);
            return;
        }

        // Player must be holding something
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(Messages.MUST_HOLD_ITEM);
            return;
        }

        // Take exactly one item from the stack, leave the rest
        ItemStack prize = held.clone();
        prize.setAmount(1);

        if (held.getAmount() > 1) {
            held.setAmount(held.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        Optional<Raffle> started = raffleManager.startRaffle(prize, player.getUniqueId());

        if (started.isPresent()) {
            sender.getServer().broadcastMessage(
                    Messages.raffleStarted(started.get().getPrizeName()));
        } else {
            // Race condition safety — another raffle snuck in between the check and start
            player.sendMessage(Messages.RAFFLE_ALREADY_ACTIVE);
            // Return the item since we didn't use it
            player.getInventory().addItem(prize);
        }
    }

    // -------------------------------------------------------------------------
    // /raffle join
    // -------------------------------------------------------------------------

    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYERS_ONLY);
            return;
        }

        switch (raffleManager.joinRaffle(player.getUniqueId())) {
            case SUCCESS          -> player.sendMessage(Messages.JOIN_SUCCESS);
            case ALREADY_JOINED   -> player.sendMessage(Messages.ALREADY_JOINED);
            case NO_ACTIVE_RAFFLE -> player.sendMessage(Messages.NO_ACTIVE_RAFFLE);
        }
    }

    // -------------------------------------------------------------------------
    // Tab completion
    // -------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return List.of("start", "join").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
