package eu.neydev.broadcast.service;

import eu.neydev.broadcast.config.type.ConsoleMessage;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Шаблоны консоли подставляют плейсхолдеры и уходят в WARNING-лог плагина.
 */
class ConsoleServiceTest {

    private final JavaPlugin plugin = mock(JavaPlugin.class);
    private final List<LogRecord> records = new java.util.ArrayList<>();

    @BeforeEach
    void setUp() {

        Logger logger = Logger.getLogger("console-test-" + System.nanoTime());

        logger.setUseParentHandlers(false);
        logger.addHandler(new java.util.logging.Handler() {

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

    }

    @Test
    @DisplayName("Плейсхолдеры подставляются парами ключ-значение")
    void placeholdersAreSubstituted() {

        new ConsoleService(plugin).log(ConsoleMessage.INVALID_VALUE,
                "path", "channels.ad.cost",
                "value", "-100",
                "defaultValue", "1500.0");

        assertEquals(1, records.size(), "одно предупреждение на один вызов");

        String message = records.get(0).getMessage();

        assertEquals(Level.WARNING, records.get(0).getLevel());
        assertTrue(message.contains("channels.ad.cost"), message);
        assertTrue(message.contains("-100"), message);
        assertTrue(message.contains("1500.0"), message);
        assertTrue(message.contains("§e"), "шаблон окрашен: " + message);

    }

    @Test
    @DisplayName("Непарный хвост аргументов игнорируется")
    void oddPlaceholderTailIsIgnored() {

        new ConsoleService(plugin).log(ConsoleMessage.COMMAND_MISSING, "command", "bc", "лишний");

        String message = records.get(0).getMessage();

        assertTrue(message.contains("bc"), message);
        assertFalse(message.contains("{command}"), "плейсхолдер подставлен: " + message);
        assertFalse(message.contains("лишний"), "аргумент без пары не печатается");

    }

    @Test
    @DisplayName("Отсутствующий плейсхолдер оставляет фигурные скобки в шаблоне")
    void missingPlaceholderStaysInTemplate() {

        new ConsoleService(plugin).log(ConsoleMessage.LEGACY_PATH);

        assertTrue(records.get(0).getMessage().contains("{path}"), records.get(0).getMessage());
        assertEquals(1, records.size());

    }

    @Test
    @DisplayName("Сообщение без плейсхолдеров уходит одной строкой")
    void templateWithoutPlaceholdersIsLoggedAsIs() {

        new ConsoleService(plugin).log(ConsoleMessage.ECONOMY_MISSING);

        assertEquals(1, records.size());
        assertTrue(records.get(0).getMessage().contains("No Vault economy provider found"),
                records.get(0).getMessage());

    }

}
