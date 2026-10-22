package eu.neydev.broadcast.command;

import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.service.MessageService;
import eu.neydev.broadcast.service.PermissionService;
import eu.neydev.broadcast.support.StubConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Маршрутизатор служебной команды: чужие команды игнорируются, справка
 * вместо неизвестной подкоманды, права и автодополнение.
 */
class NeyBroadcastCommandTest {

    private final CommandSender sender = mock(CommandSender.class);
    private final Command command = mock(Command.class);
    private final StubConfig config = new StubConfig()
            .withMessage(MessageKey.USAGE, "usage")
            .withMessage(MessageKey.NO_PERMISSION, "denied");

    private NeyBroadcastCommand command(List<NeyBroadcastSubCommand> subCommands) {
        return new NeyBroadcastCommand(new MessageService(config),
                new PermissionService(config), subCommands);
    }

    @BeforeEach
    void setUp() {
        when(command.getName()).thenReturn("neybroadcast");
    }

    @Test
    @DisplayName("Чужая команда не обрабатывается")
    void foreignCommandIsIgnored() {

        when(command.getName()).thenReturn("bc");
        RecordingSubCommand reload = new RecordingSubCommand("reload", null, true, List.of());

        assertFalse(command(List.of(reload)).onCommand(sender, command, "bc", new String[0]));
        assertEquals(List.of(),
                command(List.of(reload)).onTabComplete(sender, command, "bc", new String[]{""}));
        assertEquals(0, reload.calls);

    }

    @Test
    @DisplayName("Без аргументов печатается справка")
    void noArgumentsPrintUsage() {

        RecordingSubCommand reload = new RecordingSubCommand("reload", null, true, List.of());

        assertTrue(command(List.of(reload)).onCommand(sender, command, "neybroadcast", new String[0]));

        verify(sender).sendMessage("usage");
        assertEquals(0, reload.calls);

    }

    @Test
    @DisplayName("Неизвестная подкоманда тоже печатает справку")
    void unknownSubCommandPrintsUsage() {

        RecordingSubCommand reload = new RecordingSubCommand("reload", null, true, List.of());

        command(List.of(reload)).onCommand(sender, command, "neybroadcast", new String[]{"fly"});

        verify(sender).sendMessage("usage");
        assertEquals(0, reload.calls);

    }

    @Test
    @DisplayName("Подкоманда находится без учета регистра и получает хвост аргументов")
    void subCommandIsCaseInsensitiveAndReceivesTheTail() {

        RecordingSubCommand reload = new RecordingSubCommand("reload", null, true, List.of());

        command(List.of(reload)).onCommand(sender, command, "neybroadcast",
                new String[]{"RELOAD", "a", "b"});

        assertEquals(1, reload.calls);
        assertArrayEquals(new String[]{"a", "b"}, reload.lastArgs);

    }

    @Test
    @DisplayName("Без прав подкоманда не выполняется")
    void subCommandWithoutPermissionIsRefused() {

        config.withPermissionsEnabled(true);
        when(sender.hasPermission(PermissionNode.RELOAD.getDefaultPermission())).thenReturn(false);

        RecordingSubCommand reload =
                new RecordingSubCommand("reload", PermissionNode.RELOAD, true, List.of());

        command(List.of(reload)).onCommand(sender, command, "neybroadcast", new String[]{"reload"});

        verify(sender).sendMessage("denied");
        assertEquals(0, reload.calls);

    }

    @Test
    @DisplayName("С правом подкоманда выполняется")
    void subCommandWithPermissionIsExecuted() {

        config.withPermissionsEnabled(true);
        when(sender.hasPermission(PermissionNode.RELOAD.getDefaultPermission())).thenReturn(true);

        RecordingSubCommand reload =
                new RecordingSubCommand("reload", PermissionNode.RELOAD, true, List.of());

        command(List.of(reload)).onCommand(sender, command, "neybroadcast", new String[]{"reload"});

        assertEquals(1, reload.calls);

    }

    @Test
    @DisplayName("Подкоманда без узла права доступна всем даже при включенной системе")
    void subCommandWithoutNodeIsPublic() {

        config.withPermissionsEnabled(true);
        RecordingSubCommand help = new RecordingSubCommand("help", null, true, List.of());

        command(List.of(help)).onCommand(sender, command, "neybroadcast", new String[]{"help"});

        assertEquals(1, help.calls);

    }

    @Test
    @DisplayName("Результат подкоманды возвращается наружу")
    void subCommandResultIsReturned() {

        RecordingSubCommand failing = new RecordingSubCommand("reload", null, false, List.of());

        assertFalse(command(List.of(failing)).onCommand(sender, command, "neybroadcast",
                new String[]{"reload"}));

    }

    @Test
    @DisplayName("Автодополнение первого аргумента фильтрует по префиксу")
    void tabCompleteFiltersByPrefix() {

        List<NeyBroadcastSubCommand> subCommands = List.of(
                new RecordingSubCommand("reload", null, true, List.of()),
                new RecordingSubCommand("info", null, true, List.of()));

        assertEquals(List.of("reload", "info"),
                command(subCommands).onTabComplete(sender, command, "neybroadcast", new String[]{""}));
        assertEquals(List.of("info"),
                command(subCommands).onTabComplete(sender, command, "neybroadcast", new String[]{"i"}));

    }

    @Test
    @DisplayName("Автодополнение не предлагает подкоманду без права")
    void tabCompleteHidesDeniedSubCommands() {

        config.withPermissionsEnabled(true);
        when(sender.hasPermission(PermissionNode.RELOAD.getDefaultPermission())).thenReturn(false);

        List<NeyBroadcastSubCommand> subCommands = List.of(
                new RecordingSubCommand("reload", PermissionNode.RELOAD, true, List.of()),
                new RecordingSubCommand("help", null, true, List.of()));

        assertEquals(List.of("help"),
                command(subCommands).onTabComplete(sender, command, "neybroadcast", new String[]{""}));
        assertEquals(List.of(),
                command(subCommands).onTabComplete(sender, command, "neybroadcast",
                        new String[]{"reload", ""}));

    }

    @Test
    @DisplayName("Второй аргумент дополняет сама подкоманда")
    void secondArgumentIsDelegatedToSubCommand() {

        List<NeyBroadcastSubCommand> subCommands = List.of(
                new RecordingSubCommand("reload", null, true, List.of("now", "later")));

        assertEquals(List.of("later"),
                command(subCommands).onTabComplete(sender, command, "neybroadcast",
                        new String[]{"reload", "l"}));

    }

    @Test
    @DisplayName("Неизвестная подкоманда во втором аргументе не дополняется")
    void unknownSubCommandCompletesToNothing() {

        List<NeyBroadcastSubCommand> subCommands = List.of(
                new RecordingSubCommand("reload", null, true, List.of("now")));

        assertEquals(List.of(),
                command(subCommands).onTabComplete(sender, command, "neybroadcast",
                        new String[]{"fly", ""}));

    }

    /**
     * Подкоманда-регистратор: запоминает факты вызова, возвращает заданный
     * результат и заданные варианты дополнения.
     */
    private static final class RecordingSubCommand implements NeyBroadcastSubCommand {

        private final String name;
        private final PermissionNode permission;
        private final boolean result;
        private final List<String> completions;

        private int calls;
        private String @Nullable [] lastArgs;

        private RecordingSubCommand(String name, @Nullable PermissionNode permission,
                                    boolean result, List<String> completions) {
            this.name = name;
            this.permission = permission;
            this.result = result;
            this.completions = completions;
        }

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public @Nullable PermissionNode getPermission() {
            return permission;
        }

        @Override
        public boolean execute(@NotNull CommandContext context, String @NotNull [] args) {
            calls++;
            lastArgs = args;
            return result;
        }

        @Override
        public @NotNull List<String> complete(@NotNull CommandContext context, String @NotNull [] args) {
            return completions;
        }

    }

}
