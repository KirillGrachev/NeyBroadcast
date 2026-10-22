package eu.neydev.broadcast.config.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Узлы прав плагина.
 *
 * Все узлы функциональные (в отличие от NeyAntiSkull здесь нет
 * bypass-прав): BC, AD и BUY открывают свои каналы рассылки, RELOAD -
 * служебную команду. Строки узлов каналов 1.x (ney_broadcast.bc и др.)
 * сохранены как дефолты: на живых серверах они уже выданы игрокам через
 * permission-плагины, и переименование прав молча закрыло бы каналы.
 *
 * Узлы каналов читаются из секции канала (channels.bc.permission, в 1.x
 * commands.bc.permission), а не из общей секции permissions: право
 * принадлежит каналу и переезжает вместе с ним.
 */
public enum PermissionNode {

    BC("bc", BroadcastChannel.BC, "ney_broadcast.bc"),
    AD("ad", BroadcastChannel.AD, "ney_broadcast.ad"),
    BUY("buy", BroadcastChannel.BUY, "ney_broadcast.buy"),
    RELOAD("reload", null, "neybroadcast.reload");

    private final String configKey;
    private final @Nullable BroadcastChannel channel;
    private final String defaultPermission;

    PermissionNode(String configKey, @Nullable BroadcastChannel channel,
                   String defaultPermission) {
        this.configKey = configKey;
        this.channel = channel;
        this.defaultPermission = defaultPermission;
    }

    public @NotNull String getConfigKey() {
        return configKey;
    }

    /**
     * Канал, которому принадлежит узел, или null для служебных узлов.
     */
    public @Nullable BroadcastChannel getChannel() {
        return channel;
    }

    public @NotNull String getDefaultPermission() {
        return defaultPermission;
    }

    /**
     * Возвращает узел права, открывающий канал.
     *
     * Switch исчерпывающий: новый канал в {@link BroadcastChannel} без
     * своей ветки здесь не скомпилируется - пары перечислений не могут
     * разойтись молча.
     *
     * @param channel канал рассылки
     * @return узел права канала
     */
    public static @NotNull PermissionNode forChannel(@NotNull BroadcastChannel channel) {
        return switch (channel) {
            case BC -> BC;
            case AD -> AD;
            case BUY -> BUY;
        };
    }

}
