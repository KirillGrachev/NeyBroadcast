package eu.neydev.broadcast.service;

import eu.neydev.broadcast.config.BroadcastConfig;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.ChannelMessageKey;
import eu.neydev.broadcast.config.type.MessageKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис сообщений: префикс, плейсхолдеры и поддержка многострочных текстов.
 *
 * Единственная точка входа по получателю - {@link CommandSender}: Player
 * является CommandSender, поэтому отдельные перегрузки не нужны (пара
 * Player/CommandSender-перегрузок делала вызов send(null, key)
 * неоднозначным на уровне компиляции). Строки уже окрашены менеджером
 * конфигурации - здесь они только собираются и подставляются.
 *
 * Канальные сообщения (usage, insufficient_funds, payment_success) живут
 * в секции канала, поэтому для них отдельная перегрузка с
 * {@link BroadcastChannel}: путь ключа зависит от канала, и в общем
 * перечислении {@link MessageKey} ему не место.
 */
public class MessageService {

    private final BroadcastConfig config;

    public MessageService(@NotNull BroadcastConfig config) {
        this.config = config;
    }

    /**
     * Отправляет глобальное сообщение без плейсхолдеров.
     *
     * @param sender получатель (игрок или консоль)
     * @param key    ключ сообщения
     */
    public void send(@Nullable CommandSender sender, @NotNull MessageKey key) {
        send(sender, key, Map.of());
    }

    /**
     * Отправляет глобальное сообщение с подстановкой плейсхолдеров.
     *
     * @param sender       получатель (игрок или консоль)
     * @param key          ключ сообщения
     * @param placeholders карта плейсхолдеров вида length -> 90
     */
    public void send(@Nullable CommandSender sender,
                     @NotNull MessageKey key,
                     @NotNull Map<String, String> placeholders) {

        if (!isDeliverable(sender)) {
            return;
        }

        for (String line : build(key, placeholders)) {
            sender.sendMessage(line);
        }

    }

    /**
     * Отправляет сообщение канала с подстановкой плейсхолдеров.
     *
     * @param sender       получатель (игрок или консоль)
     * @param channel      канал рассылки
     * @param key          ключ сообщения канала
     * @param placeholders карта плейсхолдеров вида count -> 1500
     */
    public void send(@Nullable CommandSender sender,
                     @NotNull BroadcastChannel channel,
                     @NotNull ChannelMessageKey key,
                     @NotNull Map<String, String> placeholders) {

        if (!isDeliverable(sender)) {
            return;
        }

        for (String line : build(channel, key, placeholders)) {
            sender.sendMessage(line);
        }

    }

    /**
     * Собирает готовые строки глобального сообщения с плейсхолдерами.
     *
     * Префикс плагина НЕ пришивается автоматически: он появляется только там,
     * где автор сообщения явно написал {prefix}. Так многострочные сообщения
     * не превращаются в простыню из повторяющихся префиксов.
     *
     * @param key          ключ сообщения
     * @param placeholders карта плейсхолдеров
     * @return список строк для отправки (пустой, если сообщение выключено)
     */
    public @NotNull List<String> build(@NotNull MessageKey key,
                                       @NotNull Map<String, String> placeholders) {
        return assemble(config.getMessages(key), placeholders);
    }

    /**
     * Собирает готовые строки сообщения канала с плейсхолдерами.
     *
     * @param channel      канал рассылки
     * @param key          ключ сообщения канала
     * @param placeholders карта плейсхолдеров
     * @return список строк для отправки (пустой, если сообщение выключено)
     */
    public @NotNull List<String> build(@NotNull BroadcastChannel channel,
                                       @NotNull ChannelMessageKey key,
                                       @NotNull Map<String, String> placeholders) {
        return assemble(config.getMessages(channel, key), placeholders);
    }

    private @NotNull List<String> assemble(@NotNull List<String> lines,
                                           @NotNull Map<String, String> placeholders) {

        if (lines.isEmpty()) {
            return List.of();
        }

        Map<String, String> all = new HashMap<>(placeholders);
        all.putIfAbsent("prefix", config.getMessagePrefix());

        return lines.stream()
                .map(line -> applyPlaceholders(line, all))
                .toList();

    }

    private boolean isDeliverable(@Nullable CommandSender sender) {

        if (sender == null || !config.areMessagesEnabled()) {
            return false;
        }

        // Оффлайн-игрок сообщения не получит: отправка в мертвое соединение бессмысленна
        return !(sender instanceof Player player) || player.isOnline();

    }

    private @NotNull String applyPlaceholders(@NotNull String line,
                                              @NotNull Map<String, String> placeholders) {

        String result = line;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return result;

    }

}
