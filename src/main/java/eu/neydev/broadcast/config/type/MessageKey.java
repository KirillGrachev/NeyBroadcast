package eu.neydev.broadcast.config.type;

import org.jetbrains.annotations.NotNull;

/**
 * Ключи глобальных сообщений плагина.
 *
 * Каждый ключ хранит путь в config.yml, дефолтный текст на английском и
 * признак включенности по умолчанию. Пути глобальных сообщений совпадают
 * с 1.x (messages.no_console и др.), поэтому legacyConfigKey здесь не
 * нужен: от старой конфигурации отличается только ФОРМА значения - в 1.x
 * это была плоская строка, в 2.0 секция enabled/text. Форму читает
 * ConfigManager, плоская строка продолжает работать.
 */
public enum MessageKey {

    STATE_ON("state_on", true, "&aenabled"),

    STATE_OFF("state_off", true, "&cdisabled"),

    NO_CONSOLE("no_console", true,
            "{prefix}&#ff6b6bOnly players can use the broadcast channels."),

    NO_PERMISSION("no_permission", true,
            "{prefix}&#ff6b6bYou do not have permission to do this."),

    CONTAINS_COLOR_CODE("contains_color_code", true,
            "{prefix}&#ff6b6bThe message must not contain color codes (& or §)."),

    TOO_LARGE("too_large", true,
            "{prefix}&#ff6b6bThe message must not be longer than {length} characters."),

    DISABLED("disabled", true,
            "{prefix}&#ff6b6bNeyBroadcast is disabled in the configuration."),

    CHANNEL_DISABLED("channel_disabled", true,
            "{prefix}&#ff6b6bThe /{channel} channel is disabled."),

    ECONOMY_UNAVAILABLE("economy_unavailable", true,
            "{prefix}&#ff6b6bNo economy provider is connected - paid channels cannot work."),

    PAYMENT_FAILED("payment_failed", true,
            "{prefix}&#ff6b6bThe payment did not go through: {error}"),

    USAGE("usage", true, "{prefix}&#8ce0ffNeyBroadcast commands:\n"
            + "&f/neybroadcast reload &8- &7reload the configuration\n"
            + "&f/neybroadcast info &8- &7channels, economy and permission state"),

    RELOAD("reload", true,
            "{prefix}&#9dff8cConfiguration reloaded &7({time} ms, {channels} of {total} channels enabled)."),

    INFO("info", true, "{prefix}&7Version: &f{version}\n"
            + "&7Channels: &f{channels}\n"
            + "&7Economy: &f{economy} &8({provider})\n"
            + "&7Permissions: &f{permissions}");

    private final String configKey;
    private final boolean defaultEnabled;
    private final String defaultMessage;

    MessageKey(String configKey, boolean defaultEnabled, String defaultMessage) {
        this.configKey = configKey;
        this.defaultEnabled = defaultEnabled;
        this.defaultMessage = defaultMessage;
    }

    public @NotNull String getConfigKey() {
        return configKey;
    }

    /**
     * Включено ли сообщение, если его секции нет в конфигурации.
     */
    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public @NotNull String getDefaultMessage() {
        return defaultMessage;
    }

}
