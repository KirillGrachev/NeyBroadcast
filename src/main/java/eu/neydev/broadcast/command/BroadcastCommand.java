package eu.neydev.broadcast.command;

import eu.neydev.broadcast.config.BroadcastConfig;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.service.BroadcastService;
import eu.neydev.broadcast.service.PermissionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * Обработчик команд каналов рассылки (/bc, /ad, /buy).
 *
 * Один экземпляр обслуживает все три команды: канал определяется по
 * каноническому имени команды (Bukkit приводит алиасы broadcast и
 * advertising к нему до вызова), и в сервис уходит уже типизированный
 * {@link BroadcastChannel}, а не строка, по которой 1.x переключалась в
 * каждом геттере конфигурации.
 *
 * Автодополнение предлагает варианты из settings.suggestions (в 1.x три
 * русские фразы были зашиты в код) и только тому, у кого есть право
 * канала: подсказывать команду, которая ответит «нет прав», - значит
 * провоцировать лишний отказ.
 */
public class BroadcastCommand implements TabExecutor {

    private final BroadcastService broadcastService;
    private final BroadcastConfig config;
    private final PermissionService permissionService;

    public BroadcastCommand(@NotNull BroadcastService broadcastService,
                            @NotNull BroadcastConfig config,
                            @NotNull PermissionService permissionService) {
        this.broadcastService = broadcastService;
        this.config = config;
        this.permissionService = permissionService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {

        BroadcastChannel channel = BroadcastChannel.fromCommand(command.getName());

        if (channel == null) {
            return false;
        }

        broadcastService.handle(channel, sender, args);
        return true;

    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender,
                                               @NotNull Command command,
                                               @NotNull String alias,
                                               String @NotNull [] args) {

        BroadcastChannel channel = BroadcastChannel.fromCommand(command.getName());

        if (channel == null || args.length != 1
                || !permissionService.has(sender, PermissionNode.forChannel(channel))) {
            return List.of();
        }

        return filter(config.getSuggestions(), args[0]);

    }

    private @NotNull List<String> filter(@NotNull List<String> values, @NotNull String argument) {
        String prefix = argument.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

}
