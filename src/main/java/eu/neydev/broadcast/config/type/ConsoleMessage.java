package eu.neydev.broadcast.config.type;

import org.jetbrains.annotations.NotNull;

/**
 * Ключи сообщений консоли.
 *
 * Шаблоны зашиты в код и не читаются из конфигурации - осознанно:
 * предупреждения о битых значениях и устаревших путях это диагностика,
 * а не контент. Если бы ими владел config.yml, серверовладелец мог бы
 * выключить их одним флагом (или вырезать секцию), и плагин начал бы
 * молча работать на дефолтах - ровно в тот момент, когда предупреждения
 * нужнее всего. Плейсхолдеры {key} подставляются парами аргументов:
 * "ключ", "значение".
 */
public enum ConsoleMessage {

    INVALID_VALUE("&eInvalid value at {path}: '{value}'. Using {defaultValue}."),

    LEGACY_PATH("&eLegacy config path {path}: use {replacement} instead."),

    COMMAND_MISSING("&eCommand {command} is missing from plugin.yml."),

    COMMAND_UNREGISTER_FAILED("&eFailed to unregister command {command}: {reason}."),

    ECONOMY_MISSING("&eNo Vault economy provider found: channels with cost > 0 will "
            + "refuse to run until one is installed. Set their cost to 0 to use them "
            + "without an economy.");

    private final String defaultTemplate;

    ConsoleMessage(String defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public @NotNull String getDefaultTemplate() {
        return defaultTemplate;
    }

}
