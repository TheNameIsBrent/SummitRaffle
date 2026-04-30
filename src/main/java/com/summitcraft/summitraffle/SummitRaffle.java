package com.summitcraft.summitraffle;

import com.summitcraft.summitraffle.command.Messages;
import com.summitcraft.summitraffle.command.RaffleCommand;
import com.summitcraft.summitraffle.config.ConfigManager;
import com.summitcraft.summitraffle.raffle.RaffleManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SummitRaffle extends JavaPlugin {

    private static SummitRaffle instance;
    private ConfigManager configManager;
    private RaffleManager raffleManager;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager(this);
        Messages.setPrefix(configManager.getPrefix());
        raffleManager = new RaffleManager(this);
        registerCommands();
        getLogger().info("SummitRaffle has been enabled!");
    }

    @Override
    public void onDisable() {
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

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RaffleManager getRaffleManager() {
        return raffleManager;
    }

    private void registerCommands() {
        RaffleCommand raffleCommand = new RaffleCommand(raffleManager);
        var cmd = getCommand("raffle");
        if (cmd != null) {
            cmd.setExecutor(raffleCommand);
            cmd.setTabCompleter(raffleCommand);
        }
    }
}
