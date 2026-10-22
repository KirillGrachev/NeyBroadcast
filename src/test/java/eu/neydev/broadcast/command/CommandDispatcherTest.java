package eu.neydev.broadcast.command;

import be.seeseemelk.mockbukkit.MockBukkit;
import eu.neydev.broadcast.service.ConsoleService;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Регистрация и вырегистрация команды: executor и tab-completer ставятся
 * парой, отсутствующая команда и сбой command map уходят в консоль,
 * а не в исключение из onDisable.
 */
class CommandDispatcherTest {

    private final JavaPlugin plugin = mock(JavaPlugin.class);
    private final PluginCommand pluginCommand = mock(PluginCommand.class);
    private final TabExecutor executor = mock(TabExecutor.class);
    private final List<LogRecord> records = new ArrayList<>();

    private ConsoleService consoleService() {

        Logger logger = Logger.getLogger("dispatcher-test-" + System.nanoTime());

        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }

        });

        when(plugin.getLogger()).thenReturn(logger);
        return new ConsoleService(plugin);

    }

    @BeforeEach
    void setUp() {
        records.clear();
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    private boolean hasWarning(String token) {
        return records.stream()
                .filter(record -> record.getLevel().equals(Level.WARNING))
                .anyMatch(record -> String.valueOf(record.getMessage()).contains(token));
    }

    @Test
    @DisplayName("Executor и tab-completer ставятся одной парой")
    void registerCommandSetsBothHandlers() {

        when(plugin.getCommand("bc")).thenReturn(pluginCommand);

        new CommandDispatcher(plugin, consoleService()).registerCommand("bc", executor);

        verify(pluginCommand).setExecutor(executor);
        verify(pluginCommand).setTabCompleter(executor);

    }

    @Test
    @DisplayName("Команды нет в plugin.yml - предупреждение в консоль, без исключения")
    void missingCommandIsReportedToConsole() {

        when(plugin.getCommand("bc")).thenReturn(null);

        new CommandDispatcher(plugin, consoleService()).registerCommand("bc", executor);

        assertTrue(hasWarning("bc"), records.toString());
        assertTrue(hasWarning("plugin.yml"), records.toString());

    }

    @Test
    @DisplayName("Вырегистрация отсутствующей команды проходит молча")
    void unregisterMissingCommandIsSilent() {

        when(plugin.getCommand("bc")).thenReturn(null);

        new CommandDispatcher(plugin, consoleService()).unregisterCommand("bc");

        assertTrue(records.isEmpty(), records.toString());

    }

    @Test
    @DisplayName("Вырегистрация убирает команду из карты сервера без предупреждений")
    void unregisterRemovesCommandFromServerMap() {

        MockBukkit.mock();

        when(plugin.getCommand("bc")).thenReturn(pluginCommand);

        new CommandDispatcher(plugin, consoleService()).unregisterCommand("bc");

        verify(pluginCommand).unregister(any());
        assertTrue(records.isEmpty(), records.toString());

    }

    @Test
    @DisplayName("Сбой command map уходит в консоль, а не роняет onDisable")
    void unregisterFailureIsReportedNotThrown() {

        MockBukkit.mock();

        when(plugin.getCommand("bc")).thenReturn(pluginCommand);
        doThrow(new IllegalStateException("command map is closed"))
                .when(pluginCommand).unregister(any());

        new CommandDispatcher(plugin, consoleService()).unregisterCommand("bc");

        assertTrue(hasWarning("bc"), records.toString());
        assertTrue(hasWarning("command map is closed"), records.toString());
        assertTrue(hasWarning("Failed to unregister"), records.toString());
        verify(pluginCommand, never()).setExecutor(any());

    }

}
