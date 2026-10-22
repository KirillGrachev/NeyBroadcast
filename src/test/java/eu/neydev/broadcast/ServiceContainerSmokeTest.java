package eu.neydev.broadcast;

import eu.neydev.broadcast.config.ConfigManager;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.economy.EconomyService;
import eu.neydev.broadcast.service.ConsoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Контейнер собирает граф один раз и передает сервисам одну и ту же
 * конфигурацию: подписчиков на reload здесь нет - снапшот подменяется
 * атомарно, и сервисы подхватывают его через интерфейс.
 */
class ServiceContainerSmokeTest {

    private final NeyBroadcast plugin = mock(NeyBroadcast.class);
    private final ConfigManager configManager = mock(ConfigManager.class);
    private final ConsoleService consoleService = mock(ConsoleService.class);
    private final EconomyService economyService = new EconomyService(null);

    private ServiceContainer container() {
        return new ServiceContainer(plugin, configManager, consoleService, economyService);
    }

    @Test
    @DisplayName("Все компоненты созданы и доступны")
    void everyComponentIsCreated() {

        ServiceContainer container = container();

        assertSame(plugin, container.getPlugin());
        assertSame(configManager, container.getConfigManager());
        assertSame(consoleService, container.getConsoleService());
        assertSame(economyService, container.getEconomyService());
        assertNotNull(container.getMessageService());
        assertNotNull(container.getPermissionService());
        assertNotNull(container.getBroadcastService());

    }

    @Test
    @DisplayName("Сервисы читают конфигурацию из контейнера, а не свою копию")
    void servicesShareTheSameConfig() {

        when(configManager.getMessages(MessageKey.USAGE)).thenReturn(List.of("shared"));
        when(configManager.areMessagesEnabled()).thenReturn(true);
        when(configManager.getMessagePrefix()).thenReturn("");

        assertEquals(List.of("shared"),
                container().getMessageService().build(MessageKey.USAGE, Map.of()));

    }

    @Test
    @DisplayName("Два контейнера не делят сервисы между собой")
    void containersDoNotShareServices() {

        ServiceContainer first = container();
        ServiceContainer second = container();

        assertNotNull(first.getBroadcastService());
        assertNotNull(second.getBroadcastService());
        assertEquals(first.getMessageService().getClass(), second.getMessageService().getClass());
        org.junit.jupiter.api.Assertions.assertNotSame(first.getMessageService(),
                second.getMessageService());

    }

}
