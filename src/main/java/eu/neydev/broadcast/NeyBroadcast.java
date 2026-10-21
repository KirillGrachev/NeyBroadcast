package eu.neydev.broadcast;

import eu.neydev.broadcast.tab.TabCompleter;
import eu.neydev.broadcast.command.BroadcastCommand;
import eu.neydev.broadcast.config.ConfigManager;
import eu.neydev.broadcast.config.type.ConsoleMessage;
import eu.neydev.broadcast.economy.EconomyService;
import eu.neydev.broadcast.service.BroadcastService;
import eu.neydev.broadcast.service.ConsoleService;
import eu.neydev.broadcast.service.MessageService;
import eu.neydev.broadcast.service.PermissionService;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.milkbowl.vault.economy.Economy;

public final class NeyBroadcast extends JavaPlugin {

    private static NeyBroadcast instance;
    private ConsoleService consoleService;
    private ConfigManager configManager;
    private EconomyService economyService;

    @Override
    public void onEnable() {

        instance = this;
        saveDefaultConfig();

        consoleService = new ConsoleService(this);
        configManager = new ConfigManager(this, consoleService);
        economyService = new EconomyService(hookEconomy());

        if (economyService.isAvailable()) {
            getLogger().info("Hooked Vault economy: " + economyService.getProviderName());
        } else {
            consoleService.log(ConsoleMessage.ECONOMY_MISSING);
        }

        MessageService messageService = new MessageService(configManager);
        PermissionService permissionService = new PermissionService(configManager);
        BroadcastService broadcastService = new BroadcastService(this, configManager,
                messageService, permissionService, economyService);
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

    /**
     * Находит провайдер экономики Vault.
     *
     * Регистрация в ServicesManager - единственная проверка: наличие самого
     * Vault-плагина ничего не значит без провайдера, а провайдер без Vault
     * не зарегистрируется. Порядок загрузки гарантирует softdepend в
     * plugin.yml.
     *
     * @return провайдер или null, если экономики на сервере нет
     */
    private @Nullable Economy hookEconomy() {

        RegisteredServiceProvider<Economy> registration =
                getServer().getServicesManager().getRegistration(Economy.class);

        return registration == null ? null : registration.getProvider();

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

    public ConsoleService getConsoleService() {
        return consoleService;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }
}
