package eu.neydev.broadcast.config.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Каналы рассылки плагина.
 *
 * Каждый канал хранит ключ секции в config.yml, имя команды, дефолтную
 * стоимость и дефолтный шаблон рассылки. Дефолты совпадают с поставляемым
 * config.yml: в 1.x код и файл расходились (звук в коде по умолчанию был
 * выключен и имел тип PLING, а файл поставлялся с включенным HAT), и
 * расхождение всплывало ровно тогда, когда файл потерян или бит.
 *
 * Шаблон канала хранится НЕокрашенным: в него подставляется текст игрока,
 * и окрашивать строку нужно после подстановки - ровно так, как это делала
 * версия 1.x (подробнее в {@link eu.neydev.broadcast.config.BroadcastConfig#getTemplate}).
 */
public enum BroadcastChannel {

    BC("bc", "bc", 0.0, List.of(
            "",
            "&7[#ff4242ANNOUNCEMENT&7] &fAuthor: &b{player}",
            "&7Text &8» &f{message}",
            "")),

    AD("ad", "ad", 1500.0, List.of(
            "",
            "&7[#63ffefADVERTISEMENT&7] &fAuthor: &b{player}",
            "&7Text &8» &f{message}",
            "")),

    BUY("buy", "buy", 2000.0, List.of(
            "",
            "&7[#8642fcPURCHASE&7] &fAuthor: &b{player}",
            "&7Text &8» &f{message}",
            ""));

    private final String configKey;
    private final String commandName;
    private final double defaultCost;

    /**
     * List.of в аргументах конструктора неизменяем, но анализатор судит по
     * объявленному типу - отсюда точечное подавление вместо замены типа.
     */
    @SuppressWarnings("ImmutableEnumChecker")
    private final List<String> defaultTemplate;

    BroadcastChannel(String configKey, String commandName,
                     double defaultCost, List<String> defaultTemplate) {
        this.configKey = configKey;
        this.commandName = commandName;
        this.defaultCost = defaultCost;
        this.defaultTemplate = defaultTemplate;
    }

    public @NotNull String getConfigKey() {
        return configKey;
    }

    /**
     * Имя команды канала из plugin.yml (без алиасов: Bukkit приводит алиас
     * к каноническому имени до вызова обработчика).
     */
    public @NotNull String getCommandName() {
        return commandName;
    }

    public double getDefaultCost() {
        return defaultCost;
    }

    public @NotNull List<String> getDefaultTemplate() {
        return defaultTemplate;
    }

    /**
     * Находит канал по имени команды.
     *
     * @param commandName каноническое имя команды
     * @return канал или null, если команда не принадлежит плагину
     */
    public static @Nullable BroadcastChannel fromCommand(@Nullable String commandName) {

        if (commandName == null) {
            return null;
        }

        String normalized = commandName.toLowerCase(Locale.ROOT);

        for (BroadcastChannel channel : values()) {
            if (channel.commandName.equals(normalized)) {
                return channel;
            }
        }

        return null;

    }

}
