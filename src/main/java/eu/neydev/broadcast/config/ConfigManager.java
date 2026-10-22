package eu.neydev.broadcast.config;

import eu.neydev.broadcast.NeyBroadcast;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.ChannelMessageKey;
import eu.neydev.broadcast.config.type.ConsoleMessage;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.config.type.SoundSettings;
import eu.neydev.broadcast.service.ConsoleService;
import eu.neydev.broadcast.util.HexColorUtil;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * Менеджер конфигурации NeyBroadcast.
 *
 * Значения читаются один раз и собираются в неизменяемый снапшот, который
 * подменяется одной volatile-записью. Версия 1.x держала тридцать шесть
 * отдельных полей (по двенадцать на канал), перезаписываемых по очереди,
 * поэтому reload можно было застать наполовину примененным (стоимость уже
 * новая, шаблон еще старый), а каждый геттер содержал switch по строке
 * имени команды с тремя ветками и дефолтом-заглушкой.
 *
 * Некорректные значения загрузку не роняют: подставляется дефолт, а в
 * консоль уходит предупреждение через {@link ConsoleService}. Дефолты
 * совпадают с поставляемым config.yml: в 1.x код и файл расходились
 * (звук в коде по умолчанию был выключен и имел тип BLOCK_NOTE_BLOCK_PLING,
 * а файл поставлялся с включенным BLOCK_NOTE_BLOCK_HAT), и расхождение
 * всплывало ровно тогда, когда файл потерян или бит.
 *
 * Конфигурации 1.x читаются без ручной миграции:
 * <ul>
 *   <li>секция канала commands.bc.* находится по старому пути, когда
 *       channels.bc.* отсутствует, - с единственной подсказкой LEGACY_PATH
 *       на канал, а не на каждый ключ;</li>
 *   <li>ключ broadcast_template читается там, где нет template;</li>
 *   <li>плоские строки сообщений 1.x (messages.no_console: "текст")
 *       читаются наравне с секциями 2.0 (enabled/text) - путь не менялся,
 *       менялась только форма значения, поэтому предупреждение не
 *       печатается: файл 1.x остается полностью валидным.</li>
 * </ul>
 * Файл на диске не переписывается.
 *
 * Источник истины один - {@code plugin.getConfig()}: отдельного парсинга
 * файла менеджер не ведет, reload использует штатный reloadConfig().
 * Поврежденный YAML не тонет в тишине: Bukkit глотает
 * InvalidConfigurationException и отдает пустую конфигурацию, поэтому файл
 * перечитывается явно и ошибка уходит в SEVERE-лог со стектрейсом.
 */
public class ConfigManager implements BroadcastConfig {

    private final NeyBroadcast plugin;
    private final ConsoleService consoleService;
    private final List<Runnable> reloadListeners = new CopyOnWriteArrayList<>();

    private static final String PATH_ENABLED = "settings.enabled";
    private static final String PATH_SUGGESTIONS = "settings.suggestions";
    private static final String PATH_PERMISSIONS_ENABLED = "permissions.enabled";
    private static final String PATH_PERMISSION_RELOAD = "permissions.reload";
    private static final String PATH_MESSAGES_ENABLED = "messages.enabled";
    private static final String PATH_MESSAGE_PREFIX = "messages.prefix";
    private static final String PATH_MESSAGES = "messages.";

    private static final String SECTION_CHANNELS = "channels.";
    private static final String SECTION_CHANNELS_LEGACY = "commands.";

    /**
     * Дефолты совпадают с поставляемым config.yml (см. javadoc класса).
     */
    private static final boolean DEFAULT_PERMISSIONS_ENABLED = true;
    private static final int DEFAULT_MAX_LENGTH = 90;
    private static final double DEFAULT_COST = 0.0;

    /** Дефолтные варианты автодополнения: те же три фразы, что 1.x показывала из кода. */
    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "Hello everyone!",
            "Come join our server!",
            "Up to 50% off today!");

    /** Окрашенные дефолты глобальных сообщений: fallback {@link #getMessages} не пересобирает строки на каждый вызов. */
    private static final Map<MessageKey, List<String>> DEFAULT_MESSAGES;

    /** Полностью собранные дефолты каналов: используются, когда секции канала нет ни в новой, ни в старой форме. */
    private static final Map<BroadcastChannel, ChannelSettings> DEFAULT_CHANNELS;

    static {

        Map<MessageKey, List<String>> defaults = new EnumMap<>(MessageKey.class);

        for (MessageKey key : MessageKey.values()) {
            defaults.put(key, coloredLines(key.getDefaultMessage()));
        }

        DEFAULT_MESSAGES = Collections.unmodifiableMap(defaults);

        Map<BroadcastChannel, ChannelSettings> channels = new EnumMap<>(BroadcastChannel.class);

        for (BroadcastChannel channel : BroadcastChannel.values()) {
            channels.put(channel, defaultChannelSettings(channel));
        }

        DEFAULT_CHANNELS = Collections.unmodifiableMap(channels);

    }

    private record ChannelSettings(boolean enabled,
                                   String permission,
                                   int maxLength,
                                   double cost,
                                   List<String> template,
                                   SoundSettings sound,
                                   Map<ChannelMessageKey, List<String>> messages) {
    }

    private record MessagingSection(boolean enabled,
                                    String prefix,
                                    Map<MessageKey, List<String>> messages) {
    }

    private record ConfigSnapshot(boolean pluginEnabled,
                                  List<String> suggestions,
                                  boolean permissionsEnabled,
                                  String reloadPermission,
                                  MessagingSection messaging,
                                  Map<BroadcastChannel, ChannelSettings> channels) {
    }

    /**
     * Текущий снапшот. Единственное изменяемое поле менеджера; подменяется
     * атомарно, поэтому составные значения никогда не читаются «наполовину».
     */
    private volatile ConfigSnapshot snapshot;

    private FileConfiguration config;

    public ConfigManager(@NotNull NeyBroadcast plugin, @NotNull ConsoleService consoleService) {

        this.plugin = plugin;
        this.consoleService = consoleService;

        plugin.saveDefaultConfig();

        this.config = plugin.getConfig();

        validateReadable();
        this.snapshot = buildSnapshot();

    }

    /**
     * Перезагружает конфигурацию и уведомляет подписанные компоненты.
     */
    public void reload() {

        plugin.reloadConfig();

        this.config = plugin.getConfig();

        validateReadable();

        this.snapshot = buildSnapshot();

        reloadListeners.forEach(Runnable::run);

    }

    /**
     * Подписывает компонент на перезагрузку конфигурации.
     *
     * @param listener действие, выполняемое после reload
     */
    public void onReload(@NotNull Runnable listener) {
        reloadListeners.add(listener);
    }

    @Override
    public boolean isPluginEnabled() {
        return snapshot.pluginEnabled();
    }

    @Override
    public @NotNull List<String> getSuggestions() {
        return snapshot.suggestions();
    }

    @Override
    public boolean arePermissionsEnabled() {
        return snapshot.permissionsEnabled();
    }

    @Override
    public @NotNull String getPermission(@NotNull PermissionNode node) {

        BroadcastChannel channel = node.getChannel();

        if (channel == null) {
            return snapshot.reloadPermission();
        }

        return snapshot.channels().get(channel).permission();

    }

    @Override
    public boolean areMessagesEnabled() {
        return snapshot.messaging().enabled();
    }

    @Override
    public @NotNull String getMessagePrefix() {
        return snapshot.messaging().prefix();
    }

    @Override
    public @NotNull List<String> getMessages(@NotNull MessageKey key) {
        return snapshot.messaging().messages()
                .getOrDefault(key, DEFAULT_MESSAGES.getOrDefault(key, List.of()));
    }

    @Override
    public @NotNull List<String> getMessages(@NotNull BroadcastChannel channel,
                                             @NotNull ChannelMessageKey key) {
        return snapshot.channels().get(channel).messages().getOrDefault(key, List.of());
    }

    @Override
    public boolean isChannelEnabled(@NotNull BroadcastChannel channel) {
        return snapshot.channels().get(channel).enabled();
    }

    @Override
    public int getMaxLength(@NotNull BroadcastChannel channel) {
        return snapshot.channels().get(channel).maxLength();
    }

    @Override
    public double getCost(@NotNull BroadcastChannel channel) {
        return snapshot.channels().get(channel).cost();
    }

    @Override
    public @NotNull List<String> getTemplate(@NotNull BroadcastChannel channel) {
        return snapshot.channels().get(channel).template();
    }

    @Override
    public @NotNull SoundSettings getSound(@NotNull BroadcastChannel channel) {
        return snapshot.channels().get(channel).sound();
    }

    /**
     * Явно перечитывает файл, если результат штатной загрузки пуст:
     * поврежденный YAML должен кричать в консоль, а не тихо работать на дефолтах.
     */
    private void validateReadable() {

        File configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists() || !config.getKeys(false).isEmpty()) {
            return;
        }

        try {
            new YamlConfiguration().load(configFile);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "config.yml is broken and cannot be parsed - built-in defaults are used. "
                            + "Fix the file and run /neybroadcast reload.",
                    exception);
        }

    }

    private @NotNull ConfigSnapshot buildSnapshot() {

        Map<BroadcastChannel, ChannelSettings> channels = new EnumMap<>(BroadcastChannel.class);

        for (BroadcastChannel channel : BroadcastChannel.values()) {
            channels.put(channel, buildChannelSection(channel));
        }

        return new ConfigSnapshot(
                config.getBoolean(PATH_ENABLED, true),
                readSuggestions(),
                config.getBoolean(PATH_PERMISSIONS_ENABLED, DEFAULT_PERMISSIONS_ENABLED),
                readReloadPermission(),
                buildMessagingSection(),
                Collections.unmodifiableMap(channels)
        );

    }

    private @NotNull ChannelSettings buildChannelSection(@NotNull BroadcastChannel channel) {

        ConfigurationSection section = resolveChannelSection(channel);

        if (section == null) {
            return DEFAULT_CHANNELS.get(channel);
        }

        String basePath = sectionPath(section);
        PermissionNode node = PermissionNode.forChannel(channel);

        Map<ChannelMessageKey, List<String>> messages = new EnumMap<>(ChannelMessageKey.class);

        for (ChannelMessageKey key : ChannelMessageKey.values()) {
            messages.put(key, readMessageLines(section, "messages." + key.getConfigKey(),
                    key.isDefaultEnabled(), key.getDefaultMessage()));
        }

        return new ChannelSettings(
                section.getBoolean("enabled", true),
                readPermission(section, node, basePath + ".permission"),
                parsePositiveInt(section.get("max_length"), DEFAULT_MAX_LENGTH,
                        basePath + ".max_length"),
                parseNonNegativeDouble(section.get("cost"), channel.getDefaultCost(),
                        basePath + ".cost"),
                readTemplate(section, channel, basePath),
                parseSound(section, basePath),
                Collections.unmodifiableMap(messages)
        );

    }

    /**
     * Находит секцию канала: новая channels.&lt;key&gt;, иначе старая
     * commands.&lt;key&gt; с единственной подсказкой LEGACY_PATH.
     *
     * @param channel канал рассылки
     * @return секция или null, если канал не настроен вовсе
     */
    private @Nullable ConfigurationSection resolveChannelSection(@NotNull BroadcastChannel channel) {

        String path = SECTION_CHANNELS + channel.getConfigKey();
        ConfigurationSection section = config.getConfigurationSection(path);

        if (section != null) {
            return section;
        }

        String legacyPath = SECTION_CHANNELS_LEGACY + channel.getConfigKey();
        ConfigurationSection legacy = config.getConfigurationSection(legacyPath);

        if (legacy == null) {
            return null;
        }

        consoleService.log(ConsoleMessage.LEGACY_PATH,
                "path", legacyPath + ".*",
                "replacement", path + ".*");
        return legacy;

    }

    /**
     * Возвращает фактический путь секции канала для сообщений консоли:
     * секция могла приехать из старой ветки commands.*, и предупреждение
     * обязано называть тот путь, который реально прочитан.
     */
    private @NotNull String sectionPath(@NotNull ConfigurationSection section) {
        return section.getCurrentPath();
    }

    private @NotNull String readPermission(@NotNull ConfigurationSection section,
                                           @NotNull PermissionNode node,
                                           @NotNull String path) {

        String raw = section.getString("permission");

        if (raw == null) {
            return node.getDefaultPermission();
        }

        if (raw.isBlank()) {

            consoleService.log(ConsoleMessage.INVALID_VALUE,
                    "path", path,
                    "value", raw,
                    "defaultValue", node.getDefaultPermission());
            return node.getDefaultPermission();

        }

        return raw;

    }

    /**
     * Читает шаблон рассылки: новый ключ template, иначе старый
     * broadcast_template с подсказкой LEGACY_PATH, иначе дефолт канала.
     *
     * Строки шаблона НЕ окрашиваются: окрашивание происходит при отправке,
     * после подстановки текста игрока (см. {@link BroadcastConfig#getTemplate}).
     */
    private @NotNull List<String> readTemplate(@NotNull ConfigurationSection section,
                                               @NotNull BroadcastChannel channel,
                                               @NotNull String basePath) {

        Object raw = section.get("template");

        if (raw != null) {
            return readRawLines(raw);
        }

        Object legacy = section.get("broadcast_template");

        if (legacy == null) {
            return channel.getDefaultTemplate();
        }

        consoleService.log(ConsoleMessage.LEGACY_PATH,
                "path", basePath + ".broadcast_template",
                "replacement", SECTION_CHANNELS + channel.getConfigKey() + ".template");
        return readRawLines(legacy);

    }

    private @NotNull List<String> readRawLines(@NotNull Object raw) {

        if (raw instanceof List<?> list) {

            List<String> lines = new ArrayList<>(list.size());

            for (Object element : list) {
                if (element != null) {
                    lines.add(String.valueOf(element));
                }
            }

            return Collections.unmodifiableList(lines);

        }

        return List.of(String.valueOf(raw));

    }

    private @NotNull SoundSettings parseSound(@NotNull ConfigurationSection section,
                                              @NotNull String basePath) {

        ConfigurationSection sound = section.getConfigurationSection("sound");

        if (sound == null) {
            return new SoundSettings(SoundSettings.DEFAULT_ENABLED, defaultSound(),
                    SoundSettings.DEFAULT_VOLUME, SoundSettings.DEFAULT_PITCH);
        }

        return new SoundSettings(
                sound.getBoolean("enabled", SoundSettings.DEFAULT_ENABLED),
                parseSoundType(sound.get("type"), basePath + ".sound.type"),
                parseNonNegativeFloat(sound.get("volume"), SoundSettings.DEFAULT_VOLUME,
                        basePath + ".sound.volume"),
                parseNonNegativeFloat(sound.get("pitch"), SoundSettings.DEFAULT_PITCH,
                        basePath + ".sound.pitch")
        );

    }

    /**
     * Разрешает имя звука один раз при загрузке снапшота.
     *
     * В 1.x Sound.valueOf вызывался в обработчике команды и неизвестное имя
     * роняло IllegalArgumentException на каждую попытку рассылки. Здесь
     * опечатка дает предупреждение в консоль и звук по умолчанию.
     *
     * @param raw  значение из конфигурации
     * @param path путь для предупреждения
     * @return звук или null, если даже дефолтное имя не разрешается на этом ядре
     */
    private @Nullable Sound parseSoundType(@Nullable Object raw, @NotNull String path) {

        if (raw == null) {
            return defaultSound();
        }

        String name = String.valueOf(raw).trim().toUpperCase(Locale.ROOT);

        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException exception) {

            consoleService.log(ConsoleMessage.INVALID_VALUE,
                    "path", path,
                    "value", String.valueOf(raw),
                    "defaultValue", SoundSettings.DEFAULT_TYPE);
            return defaultSound();

        }

    }

    /**
     * Звук по умолчанию. api-version '1.20' гарантирует, что
     * BLOCK_NOTE_BLOCK_HAT есть в перечислении (он там с 1.13), поэтому
     * защитного catch здесь нет - на поддерживаемых ядрах он недостижим.
     */
    private static @NotNull Sound defaultSound() {
        return Sound.valueOf(SoundSettings.DEFAULT_TYPE);
    }

    private int parsePositiveInt(@Nullable Object raw, int defaultValue, @NotNull String path) {

        if (raw == null) {
            return defaultValue;
        }

        try {

            int value = Integer.parseInt(String.valueOf(raw).trim());

            if (value >= 1) {
                return value;
            }

        } catch (NumberFormatException exception) {
            // уходим в предупреждение ниже
        }

        consoleService.log(ConsoleMessage.INVALID_VALUE,
                "path", path,
                "value", String.valueOf(raw),
                "defaultValue", String.valueOf(defaultValue));
        return defaultValue;

    }

    private double parseNonNegativeDouble(@Nullable Object raw, double defaultValue,
                                          @NotNull String path) {

        if (raw == null) {
            return defaultValue;
        }

        try {

            double value = Double.parseDouble(String.valueOf(raw).trim());

            if (value >= 0.0 && Double.isFinite(value)) {
                return value;
            }

        } catch (NumberFormatException exception) {
            // уходим в предупреждение ниже
        }

        consoleService.log(ConsoleMessage.INVALID_VALUE,
                "path", path,
                "value", String.valueOf(raw),
                "defaultValue", String.valueOf(defaultValue));
        return defaultValue;

    }

    private float parseNonNegativeFloat(@Nullable Object raw, float defaultValue,
                                        @NotNull String path) {

        if (raw == null) {
            return defaultValue;
        }

        try {

            double value = Double.parseDouble(String.valueOf(raw).trim());

            if (value >= 0.0 && Double.isFinite(value)) {
                return (float) value;
            }

        } catch (NumberFormatException exception) {
            // уходим в предупреждение ниже
        }

        consoleService.log(ConsoleMessage.INVALID_VALUE,
                "path", path,
                "value", String.valueOf(raw),
                "defaultValue", String.valueOf(defaultValue));
        return defaultValue;

    }

    private @NotNull String readReloadPermission() {

        String raw = config.getString(PATH_PERMISSION_RELOAD);

        if (raw == null) {
            return PermissionNode.RELOAD.getDefaultPermission();
        }

        if (raw.isBlank()) {

            consoleService.log(ConsoleMessage.INVALID_VALUE,
                    "path", PATH_PERMISSION_RELOAD,
                    "value", raw,
                    "defaultValue", PermissionNode.RELOAD.getDefaultPermission());
            return PermissionNode.RELOAD.getDefaultPermission();

        }

        return raw;

    }

    private @NotNull List<String> readSuggestions() {

        Object raw = config.get(PATH_SUGGESTIONS);

        if (raw == null) {
            return DEFAULT_SUGGESTIONS;
        }

        if (raw instanceof List<?> list) {

            List<String> suggestions = new ArrayList<>(list.size());

            for (Object element : list) {
                if (element != null) {
                    suggestions.add(String.valueOf(element));
                }
            }

            return Collections.unmodifiableList(suggestions);

        }

        return List.of(String.valueOf(raw));

    }

    private @NotNull MessagingSection buildMessagingSection() {

        Map<MessageKey, List<String>> messages = new EnumMap<>(MessageKey.class);

        for (MessageKey key : MessageKey.values()) {
            messages.put(key, readMessageLines(config, PATH_MESSAGES + key.getConfigKey(),
                    key.isDefaultEnabled(), key.getDefaultMessage()));
        }

        return new MessagingSection(
                config.getBoolean(PATH_MESSAGES_ENABLED, true),
                HexColorUtil.color(config.getString(PATH_MESSAGE_PREFIX, "")),
                Collections.unmodifiableMap(messages)
        );

    }

    /**
     * Читает сообщение в любой из двух форм: секция 2.0 (enabled/text)
     * или плоская строка 1.x на том же пути.
     *
     * @param parent         секция-владелец: корень конфигурации для глобальных
     *                       сообщений, секция канала для канальных
     * @param key            относительный путь сообщения
     * @param defaultEnabled включенность по умолчанию
     * @param defaultMessage дефолтный текст
     * @return окрашенные строки; пустой список, если сообщение выключено
     */
    private @NotNull List<String> readMessageLines(@NotNull ConfigurationSection parent,
                                                   @NotNull String key,
                                                   boolean defaultEnabled,
                                                   @NotNull String defaultMessage) {

        ConfigurationSection section = parent.getConfigurationSection(key);

        if (section != null) {

            if (!section.getBoolean("enabled", defaultEnabled)) {
                return List.of();
            }

            return coloredText(section.get("text"), defaultMessage);

        }

        String flat = parent.getString(key);

        if (flat != null) {
            return coloredLines(flat);
        }

        return coloredLines(defaultMessage);

    }

    private @NotNull List<String> coloredText(@Nullable Object raw, @NotNull String defaultMessage) {

        if (raw == null) {
            return coloredLines(defaultMessage);
        }

        if (raw instanceof List<?> list) {

            List<String> lines = new ArrayList<>(list.size());

            for (Object element : list) {
                if (element != null) {
                    lines.add(HexColorUtil.color(String.valueOf(element)));
                }
            }

            return Collections.unmodifiableList(lines);

        }

        return coloredLines(String.valueOf(raw));

    }

    /**
     * Окрашивает текст и режет его на строки.
     *
     * Многострочные дефолты (usage, info) живут одним значением с \n,
     * а отправляются списком: так один и тот же текст одинаково читается и в
     * коде, и в config.yml, где он задан списком строк. Лимит -1 у split
     * обязателен - без него хвостовые пустые строки теряются, и дефолт
     * сообщения расходится с тем, что лежит в файле.
     */
    private static @NotNull List<String> coloredLines(@NotNull String text) {
        return List.of(HexColorUtil.color(text).split("\n", -1));
    }

    private static @NotNull ChannelSettings defaultChannelSettings(@NotNull BroadcastChannel channel) {

        Map<ChannelMessageKey, List<String>> messages = new EnumMap<>(ChannelMessageKey.class);

        for (ChannelMessageKey key : ChannelMessageKey.values()) {
            messages.put(key, coloredLines(key.getDefaultMessage()));
        }

        return new ChannelSettings(
                true,
                PermissionNode.forChannel(channel).getDefaultPermission(),
                DEFAULT_MAX_LENGTH,
                channel.getDefaultCost(),
                channel.getDefaultTemplate(),
                new SoundSettings(SoundSettings.DEFAULT_ENABLED, defaultSound(),
                        SoundSettings.DEFAULT_VOLUME, SoundSettings.DEFAULT_PITCH),
                Collections.unmodifiableMap(messages)
        );

    }

}
