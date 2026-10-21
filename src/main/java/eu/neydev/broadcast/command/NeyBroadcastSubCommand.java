package eu.neydev.broadcast.command;

import eu.neydev.broadcast.config.type.PermissionNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Контракт подкоманды (/neybroadcast &lt;подкоманда&gt;).
 *
 * Намеренно узкий: ни признака «только для игроков», ни «скрытой»
 * подкоманды здесь нет - обе ветки маршрутизатора были бы недостижимы,
 * а мертвый код семья убирает, а не копит. Обе подкоманды (reload, info)
 * доступны и консоли. Нужная подкоманда добавит флаг вместе с первой
 * реальной реализацией.
 */
public interface NeyBroadcastSubCommand {

    /**
     * Возвращает название подкоманды.
     *
     * @return название
     */
    @NotNull String getName();

    /**
     * Возвращает узел права доступа.
     *
     * @return узел права или null, если подкоманда доступна всем
     */
    @Nullable PermissionNode getPermission();

    /**
     * Выполняет подкоманду.
     *
     * @param context контекст вызова
     * @param args    аргументы после названия подкоманды
     * @return true если команда обработана
     */
    boolean execute(@NotNull CommandContext context, String @NotNull [] args);

    /**
     * Возвращает варианты автодополнения подкоманды.
     *
     * @param context контекст вызова
     * @param args    аргументы после названия подкоманды
     * @return варианты автодополнения
     */
    default @NotNull List<String> complete(@NotNull CommandContext context, String @NotNull [] args) {
        return List.of();
    }

}
