package eu.neydev.broadcast.command;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Контекст вызова команды: игрок выделяется из отправителя, консоль
 * остается без игрока и не проходит requirePlayer.
 */
class CommandContextTest {

    private final Player player = mock(Player.class);
    private final ConsoleCommandSender console = mock(ConsoleCommandSender.class);

    @Test
    @DisplayName("Отправитель-игрок дает и sender, и player")
    void playerSenderCarriesPlayer() {

        CommandContext context = CommandContext.of(player);

        assertSame(player, context.sender());
        assertSame(player, context.player());
        assertTrue(context.isPlayer());
        assertSame(player, context.requirePlayer());

    }

    @Test
    @DisplayName("Консоль остается без игрока")
    void consoleSenderHasNoPlayer() {

        CommandContext context = CommandContext.of(console);

        assertSame(console, context.sender());
        assertNull(context.player());
        assertFalse(context.isPlayer());

    }

    @Test
    @DisplayName("requirePlayer из консоли падает с понятным сообщением")
    void requirePlayerFromConsoleThrows() {

        CommandContext context = CommandContext.of(console);
        IllegalStateException exception = assertThrows(IllegalStateException.class, context::requirePlayer);

        assertEquals("Player-only subcommand from console", exception.getMessage());

    }

}
