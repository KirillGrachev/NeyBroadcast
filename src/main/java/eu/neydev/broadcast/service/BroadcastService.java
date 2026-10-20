package eu.neydev.broadcast.service;

import eu.neydev.broadcast.config.BroadcastConfig;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.ChannelMessageKey;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.config.type.SoundSettings;
import eu.neydev.broadcast.util.HexColorUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис рассылки канала.
 *
 * Порядок проверок повторяет 1.x дословно - консоль, пустые аргументы,
 * право, цветовые коды, длина, баланс, списание, рассылка, звук:
 * серверовладельцы знают этот порядок по текстам, которые получали их
 * игроки, и менять его без причины нельзя. Сверху добавлены только два
 * новых мастер-выключателя (плагин и канал).
 *
 * Шаблон окрашивается ПОСЛЕ подстановки текста игрока: HEX-коды в тексте
 * игрока проходят тот же путь, что и в 1.x (символы & и § отклоняются
 * проверкой выше, а #RRGGBB разрешен и окрашивается).
 */
public class BroadcastService {

    private final Plugin plugin;
    private final BroadcastConfig config;
    private final Economy economy;

    public BroadcastService(@NotNull Plugin plugin,
                            @NotNull BroadcastConfig config,
                            @NotNull Economy economy) {

        this.plugin = plugin;
        this.config = config;
        this.economy = economy;

    }

    /**
     * Обрабатывает вызов команды канала по ее имени.
     *
     * @param sender      отправитель команды
     * @param commandName каноническое имя команды
     * @param args        аргументы команды
     */
    public void handleCommand(@NotNull CommandSender sender,
                              @NotNull String commandName,
                              String @NotNull [] args) {

        BroadcastChannel channel = BroadcastChannel.fromCommand(commandName);

        if (channel == null) {
            return;
        }

        handle(channel, sender, args);

    }

    /**
     * Проводит текст игрока через все проверки канала и рассылает его.
     *
     * @param channel канал рассылки
     * @param sender  отправитель команды
     * @param args    аргументы команды (текст рассылки)
     */
    public void handle(@NotNull BroadcastChannel channel,
                       @NotNull CommandSender sender,
                       String @NotNull [] args) {

        if (!config.isPluginEnabled()) {

            send(sender, MessageKey.DISABLED, Map.of());
            return;

        }

        if (!config.isChannelEnabled(channel)) {

            send(sender, MessageKey.CHANNEL_DISABLED,
                    Map.of("channel", channel.getCommandName()));
            return;

        }

        if (!(sender instanceof Player player)) {

            send(sender, MessageKey.NO_CONSOLE, Map.of());
            return;

        }

        if (args.length == 0) {

            send(player, channel, ChannelMessageKey.USAGE,
                    Map.of("channel", channel.getCommandName()));
            return;

        }

        String message = String.join(" ", args);

        if (!hasChannelPermission(player, channel)) {

            send(player, MessageKey.NO_PERMISSION, Map.of());
            return;

        }

        if (message.contains("&") || message.contains("§")) {

            send(player, MessageKey.CONTAINS_COLOR_CODE, Map.of());
            return;

        }

        int maxLength = config.getMaxLength(channel);

        if (message.length() > maxLength) {

            send(player, MessageKey.TOO_LARGE,
                    Map.of("length", String.valueOf(maxLength)));
            return;

        }

        double cost = config.getCost(channel);

        if (cost > 0.0 && economy.getBalance(player) < cost) {

            send(player, channel, ChannelMessageKey.INSUFFICIENT_FUNDS,
                    moneyPlaceholders(player, channel, cost));
            return;

        }

        if (cost > 0.0) {

            economy.withdrawPlayer(player, cost);

            send(player, channel, ChannelMessageKey.PAYMENT_SUCCESS,
                    moneyPlaceholders(player, channel, cost));

        }

        broadcast(channel, player, message);
        playSound(channel, player);

    }

    /**
     * Рассылает строки шаблона всем игрокам в сети.
     *
     * Окраска и подстановка выполняются построчно и в одном порядке с 1.x:
     * сначала плейсхолдеры, потом цвета - иначе HEX-коды из текста игрока
     * остались бы литералом.
     */
    private void broadcast(@NotNull BroadcastChannel channel,
                           @NotNull Player player,
                           @NotNull String message) {

        for (String line : config.getTemplate(channel)) {

            String rendered = HexColorUtil.color(line
                    .replace("{player}", player.getName())
                    .replace("%player%", player.getName())
                    .replace("{message}", message)
                    .replace("%message%", message));

            for (Player online : plugin.getServer().getOnlinePlayers()) {
                online.sendMessage(rendered);
            }

        }

    }

    private void playSound(@NotNull BroadcastChannel channel, @NotNull Player player) {

        SoundSettings sound = config.getSound(channel);

        if (!sound.isPlayable()) {
            return;
        }

        player.playSound(player.getLocation(), sound.sound(), sound.volume(), sound.pitch());

    }

    private boolean hasChannelPermission(@NotNull Player player,
                                         @NotNull BroadcastChannel channel) {

        if (!config.arePermissionsEnabled()) {
            return true;
        }

        return player.hasPermission(config.getPermission(PermissionNode.forChannel(channel)));

    }

    /**
     * Плейсхолдеры денежных сообщений канала.
     *
     * {count} - целое число монет (формат 1.x), {cost} - то же значение
     * в формате строки; {player} и {player_name} - имя автора рассылки
     * ({player_name} - плейсхолдер 1.x, живые конфигурации содержат его).
     */
    private @NotNull Map<String, String> moneyPlaceholders(@NotNull Player player,
                                                           @NotNull BroadcastChannel channel,
                                                           double cost) {

        Map<String, String> placeholders = new HashMap<>();

        placeholders.put("channel", channel.getCommandName());
        placeholders.put("player", player.getName());
        placeholders.put("player_name", player.getName());
        placeholders.put("count", String.valueOf((int) cost));
        placeholders.put("cost", String.valueOf((int) cost));

        return placeholders;

    }

    private void send(@NotNull CommandSender sender,
                      @NotNull MessageKey key,
                      @NotNull Map<String, String> placeholders) {

        for (String line : build(config.getMessages(key), placeholders)) {
            sender.sendMessage(line);
        }

    }

    private void send(@NotNull CommandSender sender,
                      @NotNull BroadcastChannel channel,
                      @NotNull ChannelMessageKey key,
                      @NotNull Map<String, String> placeholders) {

        for (String line : build(config.getMessages(channel, key), placeholders)) {
            sender.sendMessage(line);
        }

    }

    private @NotNull List<String> build(@NotNull List<String> lines,
                                        @NotNull Map<String, String> placeholders) {

        if (lines.isEmpty() || !config.areMessagesEnabled()) {
            return List.of();
        }

        Map<String, String> all = new HashMap<>(placeholders);
        all.putIfAbsent("prefix", config.getMessagePrefix());

        return lines.stream()
                .map(line -> applyPlaceholders(line, all))
                .toList();

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
