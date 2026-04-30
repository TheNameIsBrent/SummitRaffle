package com.summitcraft.summitraffle;

import com.summitcraft.summitraffle.command.RaffleCommand;
import com.summitcraft.summitraffle.raffle.RaffleManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SummitRaffle extends JavaPlugin {

    private static SummitRaffle instance;
    private RaffleManager raffleManager;

    @Override
    public void onEnable() {
        instance = this;
        raffleManager = new RaffleManager(getLogger());
        registerCommands();
        getLogger().info("SummitRaffle has been enabled!");
    }

    @Override
    public void onDisable() {
        // If a raffle is running when the server shuts down, stop it cleanly.
        if (raffleManager != null && raffleManager.isRaffleActive()) {
            raffleManager.stopRaffle();
            getLogger().warning("Server shut down with an active raffle — raffle cancelled.");
        }
        getLogger().info("SummitRaffle has been disabled!");
        instance = null;
    }

    public static SummitRaffle getInstance() {
        return instance;
    }

    public RaffleManager getRaffleManager() {
        return raffleManager;
    }

    // -------------------------------------------------------------------------
    // Internal setup
    // -------------------------------------------------------------------------

    private void registerCommands() {
        RaffleCommand raffleCommand = new RaffleCommand(raffleManager);
        var cmd = getCommand("raffle");
        if (cmd != null) {
            cmd.setExecutor(raffleCommand);
            cmd.setTabCompleter(raffleCommand);
        }
    }
}
