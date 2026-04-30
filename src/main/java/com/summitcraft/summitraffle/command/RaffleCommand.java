package com.summitcraft.summitraffle.command;

import com.summitcraft.summitraffle.raffle.Raffle;
import com.summitcraft.summitraffle.raffle.RaffleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Handles all /raffle subcommands.
 *
 * <p>Subcommands:</p>
 * <ul>
 *   <li>{@code /raffle start <prize>} — starts a new raffle (requires {@code raffle.start})</li>
 *   <li>{@code /raffle join}          — joins the active raffle (players only)</li>
 * </ul>
 */
public class RaffleCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_START = "raffle.start";

    private final RaffleManager raffleManager;

    public RaffleCommand(RaffleManager raffleManager) {
        this.raffleManager = raffleManager;
    }

    // -------------------------------------------------------------------------
    // Command dispatch
    // -------------------------------------------------------------------------

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> handleStart(sender, args);
            case "join"  -> handleJoin(sender);
            default      -> sendUsage(sender);
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Subcommand handlers
    // -------------------------------------------------------------------------

    private void handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_START)) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Messages.START_USAGE);
            return;
        }

        // Join remaining args to support multi-word prizes, e.g. "Diamond Sword"
        String prize = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Creator UUID: console falls back to a fixed nil UUID
        var creatorUUID = (sender instanceof Player player)
                ? player.getUniqueId()
                : java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");

        Optional<Raffle> result = raffleManager.startRaffle(prize, creatorUUID);

        if (result.isPresent()) {
            // Broadcast to the whole server so players know they can join
            sender.getServer().broadcastMessage(Messages.raffleStarted(prize));
        } else {
            sender.sendMessage(Messages.RAFFLE_ALREADY_ACTIVE);
        }
    }

    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYERS_ONLY);
            return;
        }

        switch (raffleManager.joinRaffle(player.getUniqueId())) {
            case SUCCESS           -> player.sendMessage(Messages.JOIN_SUCCESS);
            case ALREADY_JOINED    -> player.sendMessage(Messages.ALREADY_JOINED);
            case NO_ACTIVE_RAFFLE  -> player.sendMessage(Messages.NO_ACTIVE_RAFFLE);
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
            return List.of("start", "join")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return List.of("<prize>");
        }
        return List.of();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Messages.USAGE);
    }
}
