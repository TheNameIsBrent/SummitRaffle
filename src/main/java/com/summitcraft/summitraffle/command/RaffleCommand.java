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
            sender.sendMessage(Messages.usage());
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> handleStart(sender);
            case "join"  -> handleJoin(sender);
            default      -> sender.sendMessage(Messages.usage());
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // /raffle start
    // -------------------------------------------------------------------------

    private void handleStart(CommandSender sender) {
        if (!sender.hasPermission(PERM_START)) {
            sender.sendMessage(Messages.noPermission());
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.playersOnly());
            return;
        }

        if (raffleManager.isRaffleActive()) {
            player.sendMessage(Messages.raffleAlreadyActive());
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(Messages.mustHoldItem());
            return;
        }

        ItemStack prize = held.clone();
        player.getInventory().setItemInMainHand(null);

        Optional<Raffle> started = raffleManager.startRaffle(prize, player.getUniqueId(), player.getName());

        if (started.isEmpty()) {
            // Race condition safety — return the item
            player.sendMessage(Messages.raffleAlreadyActive());
            player.getInventory().addItem(prize);
        }
        // Broadcast is fired inside RaffleManager.startRaffle() — nothing to do on success
    }

    // -------------------------------------------------------------------------
    // /raffle join
    // -------------------------------------------------------------------------

    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.playersOnly());
            return;
        }

        switch (raffleManager.joinRaffle(player.getUniqueId())) {
            case SUCCESS             -> player.sendMessage(Messages.joinSuccess());
            case ALREADY_JOINED      -> player.sendMessage(Messages.alreadyJoined());
            case NO_ACTIVE_RAFFLE    -> player.sendMessage(Messages.noActiveRaffle());
            case CREATOR_CANNOT_JOIN -> player.sendMessage(Messages.creatorCannotJoin());
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
