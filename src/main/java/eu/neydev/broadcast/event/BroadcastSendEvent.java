package eu.neydev.broadcast.event;

import eu.neydev.broadcast.config.type.BroadcastChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Вызывается, когда текст игрока прошел все проверки канала, - до списания
 * денег и до отправки строк в чат.
 *
 * Два контракта Bukkit соблюдены явно:
 * 1. Событие синхронное: конструктор не передает флаг async, иначе вызов
 *    из главного потока падает с "may only be triggered asynchronously".
 * 2. HandlerList собственный и статический: общий список приводил бы к тому,
 *    что слушатель одного события получал бы все события плагина.
 *
 * Отмена означает «рассылки не будет»: деньги НЕ списываются (событие
 * намеренно вызывается до оплаты - отменять транзакцию задним числом
 * значило бы возвращать деньги вручную и гонять экономику по кругу),
 * строки не отправляются, звук не играет. Так плагины регионов, арен и
 * античита могут veto рассылку по своим правилам, не выключая канал в
 * конфигурации. Строки доступны только на чтение: содержимое рассылки
 * принадлежит конфигурации канала, а слушатель, желающий свой текст,
 * отменяет событие и отправляет его сам.
 */
public class BroadcastSendEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final BroadcastChannel channel;
    private final String message;
    private final List<String> lines;
    private final double cost;

    private boolean cancelled;

    public BroadcastSendEvent(@NotNull Player player,
                              @NotNull BroadcastChannel channel,
                              @NotNull String message,
                              @NotNull List<String> lines,
                              double cost) {

        this.player = player;
        this.channel = channel;
        this.message = message;
        this.lines = List.copyOf(lines);
        this.cost = cost;

    }

    /**
     * Игрок, запустивший рассылку.
     */
    public @NotNull Player getPlayer() {
        return player;
    }

    /**
     * Канал рассылки.
     */
    public @NotNull BroadcastChannel getChannel() {
        return channel;
    }

    /**
     * Исходный текст игрока (до подстановки в шаблон).
     */
    public @NotNull String getMessage() {
        return message;
    }

    /**
     * Готовые строки рассылки: шаблон канала с подставленным именем и
     * текстом, уже окрашенный. Только на чтение.
     */
    public @NotNull List<String> getLines() {
        return lines;
    }

    /**
     * Стоимость рассылки, которая будет списана (0.0 для бесплатного канала).
     */
    public double getCost() {
        return cost;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

}
