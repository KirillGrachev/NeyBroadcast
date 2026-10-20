package eu.neydev.broadcast.config.type;

import org.jetbrains.annotations.NotNull;

/**
 * Ключи сообщений отдельного канала рассылки.
 *
 * Живут в секции канала (channels.bc.messages.usage и т.д.), а не в
 * глобальных messages: в 1.x тексты «недостаточно средств» и «списано
 * N монет» у каждого канала были свои, и это правильно - стоимость и
 * формулировки каналов различаются. Пути совпадают с 1.x
 * (commands.bc.messages.usage), отличается только форма значения:
 * плоская строка 1.x читается наравне с секцией enabled/text.
 */
public enum ChannelMessageKey {

    USAGE("usage", true, "{prefix}&#8ce0ffUsage: /{channel} <message>"),

    INSUFFICIENT_FUNDS("insufficient_funds", true,
            "{prefix}&#ff6b6bYou do not have enough money: /{channel} costs {cost}."),

    PAYMENT_SUCCESS("payment_success", true,
            "{prefix}&#9dff8cCharged {count} coins for the /{channel} broadcast.");

    private final String configKey;
    private final boolean defaultEnabled;
    private final String defaultMessage;

    ChannelMessageKey(String configKey, boolean defaultEnabled, String defaultMessage) {
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
