package eu.neydev.broadcast;

import eu.neydev.broadcast.command.BroadcastCommand;
import eu.neydev.broadcast.command.CommandDispatcher;
import eu.neydev.broadcast.command.NeyBroadcastCommand;
import eu.neydev.broadcast.command.NeyBroadcastSubCommand;
import eu.neydev.broadcast.command.sub.InfoSubCommand;
import eu.neydev.broadcast.command.sub.ReloadSubCommand;
import eu.neydev.broadcast.config.ConfigManager;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.ConsoleMessage;
import eu.neydev.broadcast.economy.EconomyService;
import eu.neydev.broadcast.service.ConsoleService;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.milkbowl.vault.economy.Economy;

import java.util.List;

/**
 * Точка входа плагина.
 *
 * Класс не final осознанно (в 1.x он был final): интеграционные тесты
 * MockBukkit грузят плагин через прокси-подкласс, а наследование JavaPlugin
 * в продакшене никто не использует - подклассы создаются только тестовой
 * средой.
 */
public class NeyBroadcast extends JavaPlugin {

    private static final String COMMAND_CONTROL = "neybroadcast";

    private ConsoleService consoleService;
    private ConfigManager configManager;
    private EconomyService economyService;
    private ServiceContainer services;
    private @Nullable CommandDispatcher commandDispatcher;

    @Override
    public void onEnable() {

        this.consoleService = new ConsoleService(this);
        this.configManager = new ConfigManager(this, consoleService);
        this.economyService = new EconomyService(hookEconomy());

        if (economyService.isAvailable()) {
            getLogger().info("Hooked Vault economy: " + economyService.getProviderName());
        } else {
            consoleService.log(ConsoleMessage.ECONOMY_MISSING);
        }

        this.services = new ServiceContainer(this, configManager, consoleService, economyService);

        this.commandDispatcher = new CommandDispatcher(this, consoleService);

        BroadcastCommand channelCommand = new BroadcastCommand(
                services.getBroadcastService(),
                configManager,
                services.getPermissionService());

        for (BroadcastChannel channel : BroadcastChannel.values()) {
            commandDispatcher.registerCommand(channel.getCommandName(), channelCommand);
        }

        commandDispatcher.registerCommand(COMMAND_CONTROL, new NeyBroadcastCommand(
                services.getMessageService(),
                services.getPermissionService(),
                buildSubCommands()
        ));

        getLogger().info("NeyBroadcast started successfully.");

    }

    @Override
    public void onDisable() {

        if (commandDispatcher != null) {

            for (BroadcastChannel channel : BroadcastChannel.values()) {
                commandDispatcher.unregisterCommand(channel.getCommandName());
            }

            commandDispatcher.unregisterCommand(COMMAND_CONTROL);

        }

        getLogger().info("NeyBroadcast stopped.");

    }

    /**
     * Находит провайдер экономики Vault.
     *
     * Литерал Economy.class загружает класс Vault, а на сервере без Vault
     * это NoClassDefFoundError посреди onEnable - плагин, который задуман
     * работающим без экономики, не смог бы включиться вовсе. Поэтому весь
     * поиск обернут в перехват ошибки линковки: нет классов Vault - нет и
     * экономики, бесплатный канал это не волнует. Проверка по имени плагина
     * (getPlugin("Vault")) не нужна: провайдер без Vault не зарегистрируется,
     * а порядок загрузки гарантирует softdepend в plugin.yml.
     *
     * @return провайдер или null, если экономики на сервере нет
     */
    private @Nullable Economy hookEconomy() {

        try {

            RegisteredServiceProvider<Economy> registration =
                    getServer().getServicesManager().getRegistration(Economy.class);

            return registration == null ? null : registration.getProvider();

        } catch (NoClassDefFoundError error) {
            // Vault отсутствует на сервере: класс Economy не загрузить
            return null;
        }

    }

    /**
     * Собирает подкоманды: команда получает зависимости из контейнера,
     * а не тянет их из плагина сама.
     */
    private @NotNull List<NeyBroadcastSubCommand> buildSubCommands() {
        return List.of(
                new ReloadSubCommand(configManager, services.getMessageService()),
                new InfoSubCommand(this, configManager, economyService,
                        services.getMessageService())
        );
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

    public ServiceContainer getServices() {
        return services;
    }

}
