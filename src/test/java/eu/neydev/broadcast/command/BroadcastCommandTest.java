package eu.neydev.broadcast.command;

import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.service.BroadcastService;
import eu.neydev.broadcast.service.PermissionService;
import eu.neydev.broadcast.support.StubConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Маршрутизатор каналов: команда приводит имя к типизированному каналу,
 * чужие команды игнорируются, автодополнение выдается только с правом
 * канала и только для первого аргумента.
 */
class BroadcastCommandTest {

    private final BroadcastService service = mock(BroadcastService.class);
    private final CommandSender sender = mock(CommandSender.class);
    private final Command command = mock(Command.class);
    private final StubConfig config = new StubConfig()
            .withSuggestions("Hello everyone!", "Come join our server!");

    private BroadcastCommand router() {
        return new BroadcastCommand(service, config, new PermissionService(config));
    }

    private Command named(String name) {
        when(command.getName()).thenReturn(name);
        return command;
    }

    @Test
    @DisplayName("Имя команды приводит к каналу и уходит в сервис типизированным")
    void commandNameIsRoutedToItsChannel() {

        assertTrue(router().onCommand(sender, named("bc"), "bc", new String[]{"hello"}));

        verify(service).handle(eq(BroadcastChannel.BC), eq(sender), any(String[].class));

    }

    @Test
    @DisplayName("Каждый из трех каналов маршрутизируется своим")
    void everyChannelIsRouted() {

        router().onCommand(sender, named("ad"), "ad", new String[]{"x"});
        router().onCommand(sender, named("buy"), "buy", new String[]{"y"});

        verify(service).handle(eq(BroadcastChannel.AD), eq(sender), any(String[].class));
        verify(service).handle(eq(BroadcastChannel.BUY), eq(sender), any(String[].class));

    }

    @Test
    @DisplayName("Чужая команда не обрабатывается")
    void foreignCommandIsIgnored() {

        assertFalse(router().onCommand(sender, named("gps"), "gps", new String[0]));
        assertEquals(List.of(), router().onTabComplete(sender, named("gps"), "gps", new String[]{""}));

        verifyNoInteractions(service);

    }

    @Test
    @DisplayName("Пустые аргументы уходят в сервис как есть: там своя ветка usage")
    void emptyArgumentsReachTheService() {

        router().onCommand(sender, named("bc"), "bc", new String[0]);

        verify(service).handle(eq(BroadcastChannel.BC), eq(sender), eq(new String[0]));

    }

    @Test
    @DisplayName("Автодополнение первого аргумента предлагает варианты из конфига")
    void firstArgumentCompletesFromSuggestions() {

        config.withPermissionsEnabled(false);

        assertEquals(List.of("Hello everyone!", "Come join our server!"),
                router().onTabComplete(sender, named("bc"), "bc", new String[]{""}));

    }

    @Test
    @DisplayName("Варианты фильтруются по префиксу без учета регистра")
    void suggestionsAreFilteredByPrefix() {

        config.withPermissionsEnabled(false);

        assertEquals(List.of("Come join our server!"),
                router().onTabComplete(sender, named("bc"), "bc", new String[]{"come"}));
        assertEquals(List.of(),
                router().onTabComplete(sender, named("bc"), "bc", new String[]{"zzz"}));

    }

    @Test
    @DisplayName("Второй аргумент не дополняется: текст рассылки непредсказуем")
    void secondArgumentIsNotCompleted() {

        config.withPermissionsEnabled(false);

        assertEquals(List.of(),
                router().onTabComplete(sender, named("bc"), "bc", new String[]{"hello", ""}));

    }

    @Test
    @DisplayName("Без права канала автодополнение молчит")
    void completionRequiresChannelPermission() {

        config.withPermissionsEnabled(true);
        when(sender.hasPermission(PermissionNode.BC.getDefaultPermission())).thenReturn(false);

        assertEquals(List.of(),
                router().onTabComplete(sender, named("bc"), "bc", new String[]{""}));

    }

    @Test
    @DisplayName("С правом канала автодополнение работает")
    void completionWorksWithChannelPermission() {

        config.withPermissionsEnabled(true);
        when(sender.hasPermission(PermissionNode.BC.getDefaultPermission())).thenReturn(true);

        assertEquals(List.of("Hello everyone!"),
                router().onTabComplete(sender, named("bc"), "bc", new String[]{"Hello"}));

    }

}
