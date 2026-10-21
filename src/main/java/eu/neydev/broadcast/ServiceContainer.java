package eu.neydev.broadcast;

import eu.neydev.broadcast.config.ConfigManager;
import eu.neydev.broadcast.economy.EconomyService;
import eu.neydev.broadcast.service.BroadcastService;
import eu.neydev.broadcast.service.ConsoleService;
import eu.neydev.broadcast.service.MessageService;
import eu.neydev.broadcast.service.PermissionService;
import org.jetbrains.annotations.NotNull;

/**
 * Контейнер компонентов плагина.
 *
 * Порядок создания зафиксирован: каждый сервис получает уже готовые
 * зависимости и никто не создает их себе сам. Версия 1.x собирала граф
 * в onEnable и раздавала его наружу через статический getInstance() -
 * ни подменить в тесте, ни переиспользовать в команде компоненты было
 * нельзя.
 */
public final class ServiceContainer {

    private final NeyBroadcast plugin;
    private final ConfigManager configManager;
    private final ConsoleService consoleService;
    private final EconomyService economyService;

    private final MessageService messageService;
    private final PermissionService permissionService;
    private final BroadcastService broadcastService;

    public ServiceContainer(@NotNull NeyBroadcast plugin,
                            @NotNull ConfigManager configManager,
                            @NotNull ConsoleService consoleService,
                            @NotNull EconomyService economyService) {

        this.plugin = plugin;
        this.configManager = configManager;
        this.consoleService = consoleService;
        this.economyService = economyService;

        // Базовый слой: сообщения и права читают только конфигурацию
        this.messageService = new MessageService(configManager);
        this.permissionService = new PermissionService(configManager);

        // Слой рассылки: сводит конфигурацию, сообщения, права и экономику
        this.broadcastService = new BroadcastService(plugin, configManager,
                messageService, permissionService, economyService);

    }

    public NeyBroadcast getPlugin() {
        return plugin;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ConsoleService getConsoleService() {
        return consoleService;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public PermissionService getPermissionService() {
        return permissionService;
    }

    public BroadcastService getBroadcastService() {
        return broadcastService;
    }

}
