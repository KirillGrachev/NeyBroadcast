package com.ney.broadcast;

import com.ney.broadcast.tab.TabCompleter;
import com.ney.broadcast.command.BroadcastCommand;
import com.ney.broadcast.config.ConfigManager;
import com.ney.broadcast.service.BroadcastService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import net.milkbowl.vault.economy.Economy;

public final class NeyBroadcast extends JavaPlugin {

    private static NeyBroadcast instance;
    private Economy economy;
    private ConfigManager configManager;

    @Override
    public void onEnable() {

        instance = this;
        saveDefaultConfig();

        configManager = new ConfigManager(this);

        setupVault();
        if (economy == null) {

            getLogger().severe("Vault economy not available. Disabling plugin.");

            setEnabled(false);
            return;

        }

        BroadcastService broadcastService = new BroadcastService(configManager, economy);
        BroadcastCommand commands = new BroadcastCommand(broadcastService);

        TabCompleter tabCompleter = new TabCompleter();

        registerBroadcastCommands(new String[] {"bc", "ad", "buy"},
                commands, tabCompleter);

        getLogger().info("NeyBroadcast successfully enabled!");

    }

    private void setupVault() {

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {

            getLogger().warning("Vault not found! Make sure you have Vault installed.");
            return;

        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {

            getLogger().warning("Economy provider not found! Make sure you have an economy plugin with Vault support.");
            return;

        }

        economy = rsp.getProvider();
        getLogger().info("Vault connected successfully!");

    }

    private void registerBroadcastCommands(String @NotNull [] commands,
                                           BroadcastCommand broadcastCommand,
                                           TabCompleter tabCompleter) {

        for (String cmd : commands) {

            getCommand(cmd).setExecutor(broadcastCommand);
            getCommand(cmd).setTabCompleter(tabCompleter);

        }
    }

    public static NeyBroadcast getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}