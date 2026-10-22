package eu.neydev.broadcast.config;

import eu.neydev.broadcast.NeyBroadcast;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.ChannelMessageKey;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.config.type.SoundSettings;
import eu.neydev.broadcast.service.ConsoleService;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Проверка менеджера конфигурации на настоящем YAML-файле: дефолты, чтение
 * секций, сырые шаблоны, устаревшие пути и плоские сообщения 1.x, битые
 * значения, предупреждения и перезагрузка.
 *
 * Сервер не поднимается: плагин мокается, getDataFolder указывает на
 * временный каталог, а getConfig перечитывает файл на каждый вызов - так
 * reload() менеджера ведет себя как на живом ядре.
 */
class ConfigManagerTest {

    @TempDir
    Path tempDir;

    private final List<LogRecord> consoleRecords = new ArrayList<>();

    private NeyBroadcast plugin() {

        NeyBroadcast plugin = mock(NeyBroadcast.class);

        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(countingLogger());
        // getConfig перечитывает файл на каждый вызов: reload() дергает
        // штатный reloadConfig (мок-noop) и забирает свежий конфиг
        when(plugin.getConfig()).thenAnswer(answer -> loadFromDisk());
        return plugin;

    }

    private ConfigManager manager() {
        return manager("");
    }

    private ConfigManager manager(String yaml) {

        writeConfig(yaml);

        NeyBroadcast plugin = plugin();
        return new ConfigManager(plugin, new ConsoleService(plugin));

    }

    /**
     * Читает config.yml из tempDir без Bukkit-логгера: в тестах сервера нет,
     * а штатный loadConfiguration при битом YAML полез бы в Bukkit.getLogger().
     */
    private YamlConfiguration loadFromDisk() {

        YamlConfiguration loaded = new YamlConfiguration();

        try {
            loaded.load(tempDir.resolve("config.yml").toFile());
        } catch (Exception ignored) {
            // Битый или отсутствующий файл: пустая конфигурация, как в продакшене
        }

        return loaded;

    }

    private Logger countingLogger() {

        Logger logger = Logger.getLogger("broadcast-config-test-" + System.nanoTime());

        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                consoleRecords.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }

        });

        return logger;

    }

    private void writeConfig(String yaml) {
        try {
            Files.writeString(tempDir.resolve("config.yml"), yaml);
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось записать тестовый config.yml", exception);
        }
    }

    private boolean hasRecord(Level level, String token) {
        return consoleRecords.stream()
                .filter(record -> record.getLevel().equals(level))
                .anyMatch(record -> String.valueOf(record.getMessage()).contains(token));
    }

    @Test
    @DisplayName("Пустой файл дает дефолты, совпадающие с поставляемым конфигом")
    void emptyFileFallsBackToCodeDefaults() {

        ConfigManager manager = manager("");

        assertTrue(manager.isPluginEnabled());
        assertTrue(manager.arePermissionsEnabled());
        assertTrue(manager.areMessagesEnabled());
        assertEquals("", manager.getMessagePrefix());
        assertEquals(List.of("Hello everyone!", "Come join our server!", "Up to 50% off today!"),
                manager.getSuggestions());

        assertEquals(0.0, manager.getCost(BroadcastChannel.BC));
        assertEquals(1500.0, manager.getCost(BroadcastChannel.AD));
        assertEquals(2000.0, manager.getCost(BroadcastChannel.BUY));

        for (BroadcastChannel channel : BroadcastChannel.values()) {

            assertTrue(manager.isChannelEnabled(channel));
            assertEquals(90, manager.getMaxLength(channel));
            assertEquals(channel.getDefaultTemplate(), manager.getTemplate(channel));
            assertEquals(PermissionNode.forChannel(channel).getDefaultPermission(),
                    manager.getPermission(PermissionNode.forChannel(channel)));

            SoundSettings sound = manager.getSound(channel);
            assertTrue(sound.enabled());
            assertEquals(Sound.BLOCK_NOTE_BLOCK_HAT, sound.sound());
            assertEquals(0.7f, sound.volume());
            assertEquals(1.0f, sound.pitch());

        }

        assertEquals("neybroadcast.reload", manager.getPermission(PermissionNode.RELOAD));
        assertTrue(manager.getMessages(MessageKey.NO_CONSOLE).get(0).contains("Only players"),
                "дефолт сообщения на месте");
        assertTrue(manager.getMessages(BroadcastChannel.BC, ChannelMessageKey.USAGE)
                .get(0).contains("{channel}"), "канальный дефолт на месте");
        assertFalse(hasRecord(Level.SEVERE, "broken"), "пустой файл - не поломка");

    }

    @Test
    @DisplayName("Отсутствующий файл не считается битым")
    void missingFileIsNotBroken() {

        NeyBroadcast plugin = plugin();
        ConfigManager manager = new ConfigManager(plugin, new ConsoleService(plugin));

        assertTrue(manager.isPluginEnabled());
        assertFalse(hasRecord(Level.SEVERE, "broken"));

    }

    @Test
    @DisplayName("Все секции читаются из файла")
    void readsEverySection() {

        ConfigManager manager = manager("""
                settings:
                  enabled: false
                  suggestions:
                    - "One"
                    - "Two"
                permissions:
                  enabled: false
                  reload: "nb.reload"
                channels:
                  bc:
                    enabled: false
                    permission: "vip.bc"
                    max_length: 40
                    cost: 5.5
                    template:
                      - "[{player}] {message}"
                    sound:
                      enabled: false
                      type: ENTITY_ARROW_HIT_PLAYER
                      volume: 0.5
                      pitch: 1.5
                    messages:
                      usage:
                        enabled: true
                        text: "use /bc"
                      insufficient_funds:
                        enabled: false
                        text: "no money"
                messages:
                  enabled: false
                  prefix: "&8[PFX]"
                  no_console:
                    enabled: true
                    text: "console denied"
                """);

        assertFalse(manager.isPluginEnabled());
        assertEquals(List.of("One", "Two"), manager.getSuggestions());
        assertFalse(manager.arePermissionsEnabled());
        assertEquals("nb.reload", manager.getPermission(PermissionNode.RELOAD));

        assertFalse(manager.isChannelEnabled(BroadcastChannel.BC));
        assertEquals("vip.bc", manager.getPermission(PermissionNode.BC));
        assertEquals(40, manager.getMaxLength(BroadcastChannel.BC));
        assertEquals(5.5, manager.getCost(BroadcastChannel.BC));
        assertEquals(List.of("[{player}] {message}"), manager.getTemplate(BroadcastChannel.BC));

        SoundSettings sound = manager.getSound(BroadcastChannel.BC);
        assertFalse(sound.enabled());
        assertEquals(Sound.ENTITY_ARROW_HIT_PLAYER, sound.sound());
        assertEquals(0.5f, sound.volume());
        assertEquals(1.5f, sound.pitch());

        assertEquals(List.of("use /bc"),
                manager.getMessages(BroadcastChannel.BC, ChannelMessageKey.USAGE));
        assertEquals(List.of(), manager.getMessages(BroadcastChannel.BC,
                ChannelMessageKey.INSUFFICIENT_FUNDS), "выключенное сообщение пусто");

        assertFalse(manager.areMessagesEnabled());
        assertEquals("§8[PFX]", manager.getMessagePrefix(), "префикс окрашен");
        assertEquals(List.of("console denied"), manager.getMessages(MessageKey.NO_CONSOLE));

        assertFalse(hasRecord(Level.WARNING, "Legacy"), "новый формат - без legacy-предупреждений");
        assertFalse(hasRecord(Level.WARNING, "Invalid"), "все значения корректны");

    }

    @Test
    @DisplayName("Секция канала 1.x commands.* читается с одной подсказкой")
    void legacyCommandsSectionIsRead() {

        ConfigManager manager = manager("""
                commands:
                  ad:
                    permission: "old.ad"
                    max_length: 50
                    cost: 999
                    broadcast_template:
                      - "%player%: %message%"
                    sound:
                      enabled: true
                      type: BLOCK_NOTE_BLOCK_PLING
                      volume: 1.0
                      pitch: 2.0
                    messages:
                      usage: "old usage"
                      insufficient_funds: "old funds"
                      payment_success: "old success {count}"
                """);

        assertEquals("old.ad", manager.getPermission(PermissionNode.AD));
        assertEquals(50, manager.getMaxLength(BroadcastChannel.AD));
        assertEquals(999.0, manager.getCost(BroadcastChannel.AD));
        assertEquals(List.of("%player%: %message%"), manager.getTemplate(BroadcastChannel.AD));
        assertEquals(Sound.BLOCK_NOTE_BLOCK_PLING, manager.getSound(BroadcastChannel.AD).sound());
        assertEquals(2.0f, manager.getSound(BroadcastChannel.AD).pitch());
        assertEquals(List.of("old usage"),
                manager.getMessages(BroadcastChannel.AD, ChannelMessageKey.USAGE));
        assertEquals(List.of("old success {count}"),
                manager.getMessages(BroadcastChannel.AD, ChannelMessageKey.PAYMENT_SUCCESS));

        assertTrue(hasRecord(Level.WARNING, "commands.ad"), "подсказка LEGACY_PATH на секцию");
        assertFalse(hasRecord(Level.WARNING, "commands.bc"),
                "нетронутые каналы не предупреждаются");

        assertEquals(0.0, manager.getCost(BroadcastChannel.BC), "остальные каналы на дефолтах");

    }

    @Test
    @DisplayName("Новая секция channels.* побеждает старую commands.*")
    void newSectionWinsOverLegacy() {

        ConfigManager manager = manager("""
                channels:
                  bc:
                    cost: 1.0
                commands:
                  bc:
                    cost: 2.0
                """);

        assertEquals(1.0, manager.getCost(BroadcastChannel.BC));
        assertFalse(hasRecord(Level.WARNING, "commands.bc"),
                "новый ключ на месте - предупреждение не печатается");

    }

    @Test
    @DisplayName("Ключ broadcast_template читается в новой секции с подсказкой")
    void legacyTemplateKeyIsReadInsideNewSection() {

        ConfigManager manager = manager("""
                channels:
                  buy:
                    broadcast_template:
                      - "legacy line"
                """);

        assertEquals(List.of("legacy line"), manager.getTemplate(BroadcastChannel.BUY));
        assertTrue(hasRecord(Level.WARNING, "channels.buy.broadcast_template"),
                "подсказка LEGACY_PATH на ключ");

    }

    @Test
    @DisplayName("Плоские сообщения 1.x читаются без предупреждений")
    void flatLegacyMessagesAreReadSilently() {

        ConfigManager manager = manager("""
                messages:
                  no_console: "&cТолько игроки!"
                  too_large: "длинно {length}"
                """);

        assertEquals(List.of("§cТолько игроки!"), manager.getMessages(MessageKey.NO_CONSOLE));
        assertEquals(List.of("длинно {length}"), manager.getMessages(MessageKey.TOO_LARGE));
        assertFalse(hasRecord(Level.WARNING, "Legacy"),
                "путь не менялся - форма значения не повод для предупреждения");

    }

    @Test
    @DisplayName("Битые значения дают дефолты и предупреждения")
    void invalidValuesFallBackWithWarnings() {

        ConfigManager manager = manager("""
                channels:
                  bc:
                    max_length: abc
                    cost: -5
                    permission: " "
                    sound:
                      type: NOT_A_SOUND
                      volume: -1
                      pitch: xyz
                permissions:
                  reload: ""
                """);

        assertEquals(90, manager.getMaxLength(BroadcastChannel.BC));
        assertEquals(0.0, manager.getCost(BroadcastChannel.BC));
        assertEquals("ney_broadcast.bc", manager.getPermission(PermissionNode.BC));
        assertEquals("neybroadcast.reload", manager.getPermission(PermissionNode.RELOAD));

        SoundSettings sound = manager.getSound(BroadcastChannel.BC);
        assertEquals(Sound.BLOCK_NOTE_BLOCK_HAT, sound.sound());
        assertEquals(0.7f, sound.volume());
        assertEquals(1.0f, sound.pitch());

        assertTrue(hasRecord(Level.WARNING, "channels.bc.max_length"));
        assertTrue(hasRecord(Level.WARNING, "channels.bc.cost"));
        assertTrue(hasRecord(Level.WARNING, "channels.bc.permission"));
        assertTrue(hasRecord(Level.WARNING, "channels.bc.sound.type"));
        assertTrue(hasRecord(Level.WARNING, "channels.bc.sound.volume"));
        assertTrue(hasRecord(Level.WARNING, "channels.bc.sound.pitch"));
        assertTrue(hasRecord(Level.WARNING, "permissions.reload"));

    }

    @Test
    @DisplayName("Нулевой и отрицательный лимит длины отклоняются")
    void nonPositiveMaxLengthFallsBack() {

        ConfigManager manager = manager("""
                channels:
                  bc:
                    max_length: 0
                """);

        assertEquals(90, manager.getMaxLength(BroadcastChannel.BC));
        assertTrue(hasRecord(Level.WARNING, "channels.bc.max_length"));

    }

    @Test
    @DisplayName("Имя звука в нижнем регистре принимается")
    void lowercaseSoundNameIsAccepted() {

        ConfigManager manager = manager("""
                channels:
                  bc:
                    sound:
                      type: block_note_block_pling
                """);

        assertEquals(Sound.BLOCK_NOTE_BLOCK_PLING, manager.getSound(BroadcastChannel.BC).sound());
        assertFalse(hasRecord(Level.WARNING, "sound.type"));

    }

    @Test
    @DisplayName("Битый YAML кричит в SEVERE и работает на дефолтах")
    void brokenYamlLogsSevere() {

        ConfigManager manager = manager("messages: [unclosed");

        assertTrue(hasRecord(Level.SEVERE, "broken"));
        assertTrue(manager.isPluginEnabled(), "дефолты подхватились");

    }

    @Test
    @DisplayName("Перезагрузка подхватывает правки файла и уведомляет подписчиков")
    void reloadPicksUpChangesAndNotifiesListeners() {

        ConfigManager manager = manager("settings:\n  enabled: true");

        AtomicInteger notifications = new AtomicInteger();
        manager.onReload(notifications::incrementAndGet);

        assertTrue(manager.isPluginEnabled());

        writeConfig("settings:\n  enabled: false");
        manager.reload();

        assertFalse(manager.isPluginEnabled());
        assertEquals(1, notifications.get());

    }

    @Test
    @DisplayName("Шаблон остается сырым, сервисные сообщения окрашиваются")
    void templateStaysRawWhileMessagesAreColored() {

        ConfigManager manager = manager("""
                channels:
                  bc:
                    template:
                      - "&7literal {message}"
                    messages:
                      usage:
                        text: "&euse /bc"
                """);

        assertEquals(List.of("&7literal {message}"), manager.getTemplate(BroadcastChannel.BC),
                "окраска шаблона отложена до отправки");
        assertEquals(List.of("§euse /bc"),
                manager.getMessages(BroadcastChannel.BC, ChannelMessageKey.USAGE));

    }

    @Test
    @DisplayName("Выключенная секция сообщения дает пустой список")
    void disabledMessageSectionGivesEmptyList() {

        ConfigManager manager = manager("""
                messages:
                  no_console:
                    enabled: false
                    text: "console denied"
                """);

        assertEquals(List.of(), manager.getMessages(MessageKey.NO_CONSOLE));

    }

    @Test
    @DisplayName("Одиночная строка шаблона и подсказок становится списком из одного элемента")
    void scalarValuesBecomeSingleElementLists() {

        ConfigManager manager = manager("""
                settings:
                  suggestions: "Solo"
                channels:
                  bc:
                    template: "one"
                """);

        assertEquals(List.of("Solo"), manager.getSuggestions());
        assertEquals(List.of("one"), manager.getTemplate(BroadcastChannel.BC));

    }

    @Test
    @DisplayName("Многострочный дефолт справки режется по переводам строк")
    void multiLineDefaultIsSplitIntoLines() {

        ConfigManager manager = manager("");

        assertEquals(3, manager.getMessages(MessageKey.USAGE).size(),
                "заголовок и две команды");

    }

    @Test
    @DisplayName("Пустые элементы списка шаблона пропускаются")
    void nullTemplateElementsAreSkipped() {

        ConfigManager manager = manager("""
                channels:
                  bc:
                    template:
                      - "line"
                      -
                      - "tail"
                """);

        assertEquals(List.of("line", "tail"), manager.getTemplate(BroadcastChannel.BC));

    }

    @Test
    @DisplayName("Секция звука без type дает дефолтный звук без предупреждения")
    void soundSectionWithoutTypeUsesDefault() {

        ConfigManager manager = manager("""
                channels:
                  bc:
                    sound:
                      enabled: true
                """);

        assertEquals(Sound.BLOCK_NOTE_BLOCK_HAT, manager.getSound(BroadcastChannel.BC).sound());
        assertFalse(hasRecord(Level.WARNING, "sound.type"));

    }

    @Test
    @DisplayName("Нечисловая стоимость падает в дефолт с предупреждением")
    void nonNumericCostFallsBack() {

        ConfigManager manager = manager("""
                channels:
                  ad:
                    cost: "free"
                """);

        assertEquals(1500.0, manager.getCost(BroadcastChannel.AD));
        assertTrue(hasRecord(Level.WARNING, "channels.ad.cost"));

    }

    @Test
    @DisplayName("Секция сообщения без text дает дефолтный текст")
    void messageSectionWithoutTextFallsBackToDefault() {

        ConfigManager manager = manager("""
                messages:
                  no_console:
                    enabled: true
                """);

        assertTrue(manager.getMessages(MessageKey.NO_CONSOLE).get(0).contains("Only players"));

    }

    @Test
    @DisplayName("Выключенный звук остается выключенным даже с валидным именем")
    void disabledSoundStaysDisabled() {

        ConfigManager manager = manager("""
                channels:
                  bc:
                    sound:
                      enabled: false
                      type: BLOCK_NOTE_BLOCK_PLING
                """);

        SoundSettings sound = manager.getSound(BroadcastChannel.BC);

        assertFalse(sound.enabled());
        assertFalse(sound.isPlayable());
        assertEquals(Sound.BLOCK_NOTE_BLOCK_PLING, sound.sound());

    }

}
