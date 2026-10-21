package eu.neydev.broadcast.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Контекст вызова команды: отправитель и игрок (если команда вызвана игроком).
 *
 * @param sender отправитель команды
 * @param player игрок или null, если команда вызвана из консоли
 */
public record CommandContext(@NotNull CommandSender sender, @Nullable Player player) {

    /**
     * Собирает контекст из отправителя, выделяя игрока, если это он.
     *
     * @param sender отправитель команды
     * @return контекст вызова
     */
    public static @NotNull CommandContext of(@NotNull CommandSender sender) {
        return new CommandContext(sender, sender instanceof Player senderPlayer ? senderPlayer : null);
    }

    public boolean isPlayer() {
        return player != null;
    }

    /**
     * Возвращает игрока для подкоманд, недоступных консоли.
     *
     * @return игрок
     * @throws IllegalStateException если подкоманда вызвана из консоли
     */
    public @NotNull Player requirePlayer() {

        if (player == null) {
            throw new IllegalStateException("Player-only subcommand from console");
        }

        return player;

    }

}
