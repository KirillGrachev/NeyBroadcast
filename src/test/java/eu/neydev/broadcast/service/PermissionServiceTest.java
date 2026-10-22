package eu.neydev.broadcast.service;

import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.support.StubConfig;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Все узлы NeyBroadcast функциональные: выключенная система прав разрешает
 * все, включенная проверяет строку узла из конфигурации канала.
 */
class PermissionServiceTest {

    private final Player player = mock(Player.class);
    private final ConsoleCommandSender console = mock(ConsoleCommandSender.class);

    @Test
    @DisplayName("Выключенная система прав разрешает всем")
    void disabledSystemAllowsEveryone() {

        PermissionService service = new PermissionService(new StubConfig().withPermissionsEnabled(false));

        assertTrue(service.has(player, PermissionNode.BC));
        assertTrue(service.has(player, PermissionNode.RELOAD));
        assertTrue(service.has(null, PermissionNode.RELOAD), "даже без отправителя");

    }

    @Test
    @DisplayName("Включенная система прав проверяет дефолтный узел канала")
    void enabledSystemChecksChannelNode() {

        StubConfig config = new StubConfig().withPermissionsEnabled(true);
        when(player.hasPermission("ney_broadcast.bc")).thenReturn(true);

        assertTrue(new PermissionService(config).has(player, PermissionNode.BC));
        verify(player).hasPermission("ney_broadcast.bc");

    }

    @Test
    @DisplayName("Узел читается из конфигурации, а не из дефолта перечисления")
    void configuredNodeOverridesDefault() {

        StubConfig config = new StubConfig()
                .withPermissionsEnabled(true)
                .withPermission(PermissionNode.AD, "donor.ads");
        when(player.hasPermission("donor.ads")).thenReturn(true);

        assertTrue(new PermissionService(config).has(player, PermissionNode.AD));
        verify(player).hasPermission("donor.ads");

    }

    @Test
    @DisplayName("Без нужного узла доступ закрыт")
    void missingNodeDeniesAccess() {

        StubConfig config = new StubConfig().withPermissionsEnabled(true);
        when(player.hasPermission("ney_broadcast.buy")).thenReturn(false);

        assertFalse(new PermissionService(config).has(player, PermissionNode.BUY));

    }

    @Test
    @DisplayName("Null-отправитель при включенной системе не проходит")
    void nullSenderIsDeniedWhenEnabled() {

        StubConfig config = new StubConfig().withPermissionsEnabled(true);

        assertFalse(new PermissionService(config).has(null, PermissionNode.BC));

    }

    @Test
    @DisplayName("Консоль проверяется так же, как игрок")
    void consoleIsCheckedLikePlayer() {

        StubConfig config = new StubConfig().withPermissionsEnabled(true);
        when(console.hasPermission("neybroadcast.reload")).thenReturn(true);

        assertTrue(new PermissionService(config).has(console, PermissionNode.RELOAD));

    }

    @Test
    @DisplayName("Каждый канал проверяется по своему узлу независимо")
    void channelNodesAreIndependent() {

        StubConfig config = new StubConfig().withPermissionsEnabled(true);
        when(player.hasPermission("ney_broadcast.bc")).thenReturn(true);
        when(player.hasPermission("ney_broadcast.ad")).thenReturn(false);

        PermissionService service = new PermissionService(config);

        assertTrue(service.has(player, PermissionNode.forChannel(BroadcastChannel.BC)));
        assertFalse(service.has(player, PermissionNode.forChannel(BroadcastChannel.AD)));

    }

}
