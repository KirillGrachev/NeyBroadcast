package eu.neydev.broadcast.service;

import eu.neydev.broadcast.config.BroadcastConfig;
import eu.neydev.broadcast.config.type.PermissionNode;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Сервис прав доступа.
 *
 * Все узлы NeyBroadcast функциональные - в отличие от NeyAntiSkull здесь
 * нет разрешающих обходов (bypass), поэтому правило одно: выключенная
 * система прав разрешает все. Узлы каналов по умолчанию выданы всем
 * (default: true в plugin.yml): платность канала регулирует стоимость,
 * а не право, и сервер без permission-плагина остается работоспособным.
 */
public class PermissionService {

    private final BroadcastConfig config;

    public PermissionService(@NotNull BroadcastConfig config) {
        this.config = config;
    }

    /**
     * Проверяет право (канал рассылки или служебная команда).
     *
     * @param sender проверяемый отправитель (игрок или консоль)
     * @param node   узел права
     * @return true если действие разрешено
     */
    public boolean has(@Nullable CommandSender sender, @NotNull PermissionNode node) {

        if (!config.arePermissionsEnabled()) {
            return true;
        }

        return sender != null && sender.hasPermission(config.getPermission(node));

    }

}
