package eu.neydev.broadcast.event;

import eu.neydev.broadcast.config.type.BroadcastChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Контракты собственного события: синхронность, свой HandlerList, отмена
 * и неизменяемость готовых строк рассылки.
 */
class BroadcastSendEventTest {

    private final Player player = mock(Player.class);

    private BroadcastSendEvent event(double cost) {
        return new BroadcastSendEvent(player, BroadcastChannel.AD, "hello",
                List.of("line one", "line two"), cost);
    }

    @Test
    @DisplayName("Событие синхронное и несет участника, канал, текст, строки и стоимость")
    void eventIsSynchronousAndCarriesValues() {

        BroadcastSendEvent event = event(1500.0);

        assertFalse(event.isAsynchronous(), "рассылка обрабатывается в главном потоке");
        assertSame(player, event.getPlayer());
        assertSame(BroadcastChannel.AD, event.getChannel());
        assertEquals("hello", event.getMessage());
        assertEquals(List.of("line one", "line two"), event.getLines());
        assertEquals(1500.0, event.getCost());

    }

    @Test
    @DisplayName("Бесплатный канал несет нулевую стоимость")
    void freeChannelCarriesZeroCost() {

        BroadcastSendEvent event = new BroadcastSendEvent(player, BroadcastChannel.BC,
                "hi", List.of("hi"), 0.0);

        assertEquals(0.0, event.getCost());

    }

    @Test
    @DisplayName("Строки события - неизменяемая копия")
    void linesAreImmutableCopy() {

        BroadcastSendEvent event = event(10.0);

        assertThrows(UnsupportedOperationException.class,
                () -> event.getLines().add("injected"));

    }

    @Test
    @DisplayName("HandlerList собственный и совпадает со статическим")
    void handlerListIsOwnAndStatic() {

        HandlerList handlers = event(0.0).getHandlers();

        assertSame(BroadcastSendEvent.getHandlerList(), handlers);
        assertSame(handlers, event(10.0).getHandlers());

    }

    @Test
    @DisplayName("Отмена по умолчанию выключена и переключается")
    void cancellationDefaultsToFalse() {

        BroadcastSendEvent event = event(0.0);

        assertFalse(event.isCancelled());

        event.setCancelled(true);

        assertTrue(event.isCancelled());

    }

}
