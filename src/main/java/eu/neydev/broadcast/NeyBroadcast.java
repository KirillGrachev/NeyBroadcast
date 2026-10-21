package eu.neydev.broadcast;

import eu.neydev.broadcast.tab.TabCompleter;
import eu.neydev.broadcast.command.BroadcastCommand;
import eu.neydev.broadcast.config.ConfigManager;
import eu.neydev.broadcast.service.BroadcastService;
import eu.neydev.broadcast.service.ConsoleService;
import eu.neydev.broadcast.service.MessageService;
import eu.neydev.broadcast.service.PermissionService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import net.milkbowl.vault.economy.Economy;

public final class NeyBroadcast extends JavaPlugin {

    private static NeyBroadcast instance;
    private Economy economy;
    private ConsoleService consoleService;
    private ConfigManager configManager;

    @Override
    public void onEnable() {

        instance = this;
        saveDefaultConfig();

        consoleService = new ConsoleService(this);
        configManager = new ConfigManager(this, consoleService);

        setupVault();
        if (economy == null) {

            getLogger().severe("Vault economy not available. Disabling plugin.");

            setEnabled(false);
            return;

        }

        MessageService messageService = new MessageService(configManager);
        PermissionService permissionService = new PermissionService(configManager);
        BroadcastService broadcastService = new BroadcastService(this, configManager,
                messageService, permissionService, economy);
        BroadcastCommand commands = new BroadcastCommand(broadcastService);

        TabCompleter tabCompleter = new TabCompleter();

        registerBroadcastCommands(new String[] {"bc", "ad", "buy"},
                commands, tabCompleter);

        getLogger().info("NeyBroadcast started successfully.");

    }

    @Override
    public void onDisable() {

        getLogger().info("NeyBroadcast stopped.");

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

    public ConsoleService getConsoleService() {
        return consoleService;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
