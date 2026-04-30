package com.summitcraft.summitraffle;

import com.summitcraft.summitraffle.command.Messages;
import com.summitcraft.summitraffle.command.RaffleCommand;
import com.summitcraft.summitraffle.config.ConfigManager;
import com.summitcraft.summitraffle.cooldown.CooldownManager;
import com.summitcraft.summitraffle.logging.LogManager;
import com.summitcraft.summitraffle.prize.PendingPrizeManager;
import com.summitcraft.summitraffle.raffle.RaffleManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SummitRaffle extends JavaPlugin {

    private static SummitRaffle instance;

    private ConfigManager configManager;
    private LogManager logManager;
    private CooldownManager cooldownManager;
    private PendingPrizeManager pendingPrizeManager;
    private RaffleManager raffleManager;

    @Override
    public void onEnable() {
        instance = this;

        configManager       = new ConfigManager(this);
        Messages.init(configManager);

        logManager          = new LogManager(this);
        cooldownManager     = new CooldownManager(configManager);
        pendingPrizeManager = new PendingPrizeManager(this);
        raffleManager       = new RaffleManager(this, configManager, logManager, pendingPrizeManager);

        registerCommands();
        getLogger().info("SummitRaffle has been enabled!");
    }

    @Override
    public void onDisable() {
        if (raffleManager != null && raffleManager.isRaffleActive()) {
            // cancelAndReturn queues the prize to the creator via PendingPrizeManager
            // so it survives the shutdown and is delivered on next login
            raffleManager.cancelAndReturn();
            getLogger().warning("Server stopped with an active raffle — prize queued for creator.");
        }
        getLogger().info("SummitRaffle has been disabled!");
        instance = null;
    }

    public static SummitRaffle getInstance()           { return instance; }
    public ConfigManager getConfigManager()            { return configManager; }
    public LogManager getLogManager()                  { return logManager; }
    public CooldownManager getCooldownManager()        { return cooldownManager; }
    public PendingPrizeManager getPendingPrizeManager(){ return pendingPrizeManager; }
    public RaffleManager getRaffleManager()            { return raffleManager; }

    private void registerCommands() {
        RaffleCommand cmd = new RaffleCommand(raffleManager, cooldownManager);
        var bukkitCmd = getCommand("raffle");
        if (bukkitCmd != null) {
            bukkitCmd.setExecutor(cmd);
            bukkitCmd.setTabCompleter(cmd);
        }
    }
}
