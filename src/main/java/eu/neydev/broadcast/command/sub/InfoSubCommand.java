package eu.neydev.broadcast.command.sub;

import eu.neydev.broadcast.command.CommandContext;
import eu.neydev.broadcast.command.NeyBroadcastSubCommand;
import eu.neydev.broadcast.config.BroadcastConfig;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.economy.EconomyService;
import eu.neydev.broadcast.service.MessageService;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Подкоманда /neybroadcast info.
 *
 * Диагностика читается из снапшота конфигурации, а не из файла: команда
 * показывает ровно те правила, которые сейчас применяет сервис рассылки.
 * Состояние экономики берется из живого {@link EconomyService}: «economy:
 * disabled» на включенном сервере означало бы, что платные каналы молча
 * отказывают, а причина видна только в логе загрузки. Все булевы значения
 * подставляются словами state_on/state_off из той же конфигурации, поэтому
 * перевод диагностики не расходится с переводом сообщений.
 */
public class InfoSubCommand implements NeyBroadcastSubCommand {

    private final Plugin plugin;
    private final BroadcastConfig config;
    private final EconomyService economyService;
    private final MessageService messageService;

    public InfoSubCommand(@NotNull Plugin plugin,
                          @NotNull BroadcastConfig config,
                          @NotNull EconomyService economyService,
                          @NotNull MessageService messageService) {
        this.plugin = plugin;
        this.config = config;
        this.economyService = economyService;
        this.messageService = messageService;
    }

    @Override
    public @NotNull String getName() {
        return "info";
    }

    @Override
    public @NotNull PermissionNode getPermission() {
        return PermissionNode.RELOAD;
    }

    @Override
    public boolean execute(@NotNull CommandContext context, String @NotNull [] args) {

        String channels = Arrays.stream(BroadcastChannel.values())
                .filter(config::isChannelEnabled)
                .map(BroadcastChannel::getCommandName)
                .collect(Collectors.joining(", "));

        messageService.send(context.sender(), MessageKey.INFO, Map.of(
                "version", plugin.getDescription().getVersion(),
                "channels", channels.isEmpty() ? "none" : channels,
                "economy", state(economyService.isAvailable()),
                "provider", economyService.getProviderName(),
                "permissions", state(config.arePermissionsEnabled())
        ));

        return true;

    }

    /**
     * Возвращает слово-фрагмент для булева значения.
     *
     * Выключенный фрагмент дает пустую строку, а не "null": диагностика
     * остается читаемой даже тогда, когда серверовладелец убрал слова.
     */
    private @NotNull String state(boolean enabled) {
        return String.join("", config.getMessages(enabled ? MessageKey.STATE_ON : MessageKey.STATE_OFF));
    }

}
