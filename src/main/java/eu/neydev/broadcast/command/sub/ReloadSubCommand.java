package eu.neydev.broadcast.command.sub;

import eu.neydev.broadcast.command.CommandContext;
import eu.neydev.broadcast.command.NeyBroadcastSubCommand;
import eu.neydev.broadcast.config.ConfigManager;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.service.MessageService;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Подкоманда /neybroadcast reload.
 *
 * Перечитывает config.yml и обновляет снапшот конфигурации одной
 * volatile-записью: подписчиков у менеджера нет, сервисы читают значения
 * через интерфейс и подхвачивают новый снапшот сами. В подтверждение
 * уходит и время, и число включенных каналов: по нему сразу видно, что
 * секция канала не потерялась при правке файла.
 */
public class ReloadSubCommand implements NeyBroadcastSubCommand {

    private final ConfigManager configManager;
    private final MessageService messageService;

    public ReloadSubCommand(@NotNull ConfigManager configManager,
                            @NotNull MessageService messageService) {
        this.configManager = configManager;
        this.messageService = messageService;
    }

    @Override
    public @NotNull String getName() {
        return "reload";
    }

    @Override
    public @NotNull PermissionNode getPermission() {
        return PermissionNode.RELOAD;
    }

    @Override
    public boolean execute(@NotNull CommandContext context, String @NotNull [] args) {

        long startedAt = System.nanoTime();
        configManager.reload();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        long enabled = Arrays.stream(BroadcastChannel.values())
                .filter(configManager::isChannelEnabled)
                .count();

        messageService.send(context.sender(), MessageKey.RELOAD, Map.of(
                "time", String.valueOf(elapsedMillis),
                "channels", String.valueOf(enabled),
                "total", String.valueOf(BroadcastChannel.values().length)
        ));

        return true;

    }

}
