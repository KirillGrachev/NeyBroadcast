package eu.neydev.broadcast.service;

import eu.neydev.broadcast.config.BroadcastConfig;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.ChannelMessageKey;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.config.type.SoundSettings;
import eu.neydev.broadcast.economy.EconomyService;
import eu.neydev.broadcast.util.HexColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис рассылки канала.
 *
 * Порядок проверок повторяет 1.x дословно - консоль, пустые аргументы,
 * право, цветовые коды, длина, баланс, списание, рассылка, звук:
 * серверовладельцы знают этот порядок по текстам, которые получали их
 * игроки, и менять его без причины нельзя. Сверху добавлены только два
 * новых мастер-выключателя (плагин и канал) и отказ платного канала при
 * неподключенной экономике - ветка, которой в 1.x не существовало, потому
 * что там плагин без Vault не доживал до команды.
 *
 * Шаблон окрашивается ПОСЛЕ подстановки текста игрока: HEX-коды в тексте
 * игрока проходят тот же путь, что и в 1.x (символы & и § отклоняются
 * проверкой выше, а #RRGGBB разрешен и окрашивается).
 */
public class BroadcastService {

    private final Plugin plugin;
    private final BroadcastConfig config;
    private final MessageService messageService;
    private final PermissionService permissionService;
    private final EconomyService economyService;

    public BroadcastService(@NotNull Plugin plugin,
                            @NotNull BroadcastConfig config,
                            @NotNull MessageService messageService,
                            @NotNull PermissionService permissionService,
                            @NotNull EconomyService economyService) {

        this.plugin = plugin;
        this.config = config;
        this.messageService = messageService;
        this.permissionService = permissionService;
        this.economyService = economyService;

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

            messageService.send(sender, MessageKey.DISABLED);
            return;

        }

        if (!config.isChannelEnabled(channel)) {

            messageService.send(sender, MessageKey.CHANNEL_DISABLED,
                    Map.of("channel", channel.getCommandName()));
            return;

        }

        if (!(sender instanceof Player player)) {

            messageService.send(sender, MessageKey.NO_CONSOLE);
            return;

        }

        if (args.length == 0) {

            messageService.send(player, channel, ChannelMessageKey.USAGE,
                    Map.of("channel", channel.getCommandName()));
            return;

        }

        String message = String.join(" ", args);

        if (!permissionService.has(player, PermissionNode.forChannel(channel))) {

            messageService.send(player, MessageKey.NO_PERMISSION);
            return;

        }

        if (message.contains("&") || message.contains("§")) {

            messageService.send(player, MessageKey.CONTAINS_COLOR_CODE);
            return;

        }

        int maxLength = config.getMaxLength(channel);

        if (message.length() > maxLength) {

            messageService.send(player, MessageKey.TOO_LARGE,
                    Map.of("length", String.valueOf(maxLength)));
            return;

        }

        double cost = config.getCost(channel);

        if (cost > 0.0 && !pay(channel, player, cost)) {
            return;
        }

        broadcast(channel, player, message);
        playSound(channel, player);

    }

    /**
     * Проводит оплату канала: доступность экономики, баланс, списание.
     *
     * Список проверки повторяет 1.x (баланс до списания), но результат
     * списания больше не выбрасывается: отклоненный EconomyResponse
     * означает отказ всей рассылки - игрок не должен получать объявление
     * бесплатно из-за того, что экономический плагин сказал «нет».
     *
     * @param channel канал рассылки
     * @param player  плательщик
     * @param cost    стоимость рассылки (больше нуля)
     * @return true если оплата прошла и рассылку можно отправлять
     */
    private boolean pay(@NotNull BroadcastChannel channel,
                        @NotNull Player player,
                        double cost) {

        if (!economyService.isAvailable()) {

            messageService.send(player, MessageKey.ECONOMY_UNAVAILABLE);
            return false;

        }

        if (!economyService.has(player, cost)) {

            messageService.send(player, channel, ChannelMessageKey.INSUFFICIENT_FUNDS,
                    moneyPlaceholders(player, channel, cost));
            return false;

        }

        EconomyService.WithdrawResult result = economyService.withdraw(player, cost);

        if (!result.success()) {

            messageService.send(player, MessageKey.PAYMENT_FAILED,
                    Map.of("error", result.error()));
            return false;

        }

        messageService.send(player, channel, ChannelMessageKey.PAYMENT_SUCCESS,
                moneyPlaceholders(player, channel, cost));
        return true;

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

    /**
     * Плейсхолдеры денежных сообщений канала.
     *
     * {count} - целое число монет (формат 1.x), {cost} - сумма,
     * отформатированная экономическим плагином (символ валюты); {player} и
     * {player_name} - имя автора рассылки ({player_name} - плейсхолдер 1.x,
     * живые конфигурации содержат его).
     */
    private @NotNull Map<String, String> moneyPlaceholders(@NotNull Player player,
                                                           @NotNull BroadcastChannel channel,
                                                           double cost) {

        Map<String, String> placeholders = new HashMap<>();

        placeholders.put("channel", channel.getCommandName());
        placeholders.put("player", player.getName());
        placeholders.put("player_name", player.getName());
        placeholders.put("count", String.valueOf((int) cost));
        placeholders.put("cost", economyService.format(cost));

        return placeholders;

    }

}
