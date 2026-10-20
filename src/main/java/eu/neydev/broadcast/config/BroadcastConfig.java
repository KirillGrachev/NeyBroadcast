package eu.neydev.broadcast.config;

import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.ChannelMessageKey;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.config.type.SoundSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Контракт конфигурации NeyBroadcast.
 * Все остальные компоненты зависят от интерфейса, а не от реализации:
 * сервисы и команды не знают, откуда приехали значения (YAML, дефолты
 * кода или подставленный в тесте стаб).
 */
public interface BroadcastConfig {

    /**
     * Мастер-выключатель плагина: при false каналы не рассылают ничего.
     */
    boolean isPluginEnabled();

    /**
     * Варианты автодополнения первого аргумента команд каналов.
     * В 1.x список был зашит в код на русском; теперь это контент,
     * и он принадлежит конфигурации.
     */
    @NotNull List<String> getSuggestions();

    boolean arePermissionsEnabled();

    /**
     * Строка узла права из конфигурации (или дефолт узла).
     *
     * @param node узел права
     * @return название права для Bukkit
     */
    @NotNull String getPermission(@NotNull PermissionNode node);

    boolean areMessagesEnabled();

    /**
     * Префикс, подставляемый в плейсхолдер {prefix}. Уже окрашен.
     */
    @NotNull String getMessagePrefix();

    /**
     * Строки глобального сообщения по ключу.
     *
     * @param key ключ сообщения
     * @return окрашенные строки; пустой список, если сообщение выключено
     */
    @NotNull List<String> getMessages(@NotNull MessageKey key);

    /**
     * Строки сообщения канала по ключу.
     *
     * @param channel канал рассылки
     * @param key     ключ сообщения канала
     * @return окрашенные строки; пустой список, если сообщение выключено
     */
    @NotNull List<String> getMessages(@NotNull BroadcastChannel channel,
                                      @NotNull ChannelMessageKey key);

    boolean isChannelEnabled(@NotNull BroadcastChannel channel);

    /**
     * Максимальная длина текста рассылки (без шаблона).
     */
    int getMaxLength(@NotNull BroadcastChannel channel);

    /**
     * Стоимость рассылки через канал; 0.0 - канал бесплатен.
     */
    double getCost(@NotNull BroadcastChannel channel);

    /**
     * Строки шаблона рассылки, БЕЗ предварительной окраски.
     *
     * В отличие от остальных текстов плагина шаблон окрашивается не при
     * загрузке конфигурации, а при отправке - после подстановки текста
     * игрока: так HEX-коды, попавшие в сообщение игрока, проходят тот же
     * путь, что и в 1.x (символы & и § в тексте игрока по-прежнему
     * отклоняются до рассылки).
     */
    @NotNull List<String> getTemplate(@NotNull BroadcastChannel channel);

    @NotNull SoundSettings getSound(@NotNull BroadcastChannel channel);

}
