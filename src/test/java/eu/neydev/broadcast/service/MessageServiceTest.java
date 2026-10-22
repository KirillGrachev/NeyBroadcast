package eu.neydev.broadcast.service;

import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.ChannelMessageKey;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.support.StubConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Доставка сообщений: префикс, плейсхолдеры, многострочность, канальные
 * ключи и отказы (получатель null, сообщения выключены, игрок оффлайн).
 */
class MessageServiceTest {

    private final Player player = mock(Player.class);
    private final ConsoleCommandSender console = mock(ConsoleCommandSender.class);

    private MessageService service(StubConfig config) {
        return new MessageService(config);
    }

    @Test
    @DisplayName("Многострочное сообщение уходит построчно с плейсхолдерами")
    void sendsEveryLineWithPlaceholders() {

        StubConfig config = new StubConfig()
                .withPrefix("&8» ")
                .withMessage(MessageKey.INFO, "{prefix}channels {channels}", "second {channels}");
        when(player.isOnline()).thenReturn(true);

        service(config).send(player, MessageKey.INFO, Map.of("channels", "bc, ad"));

        verify(player).sendMessage("&8» channels bc, ad");
        verify(player).sendMessage("second bc, ad");

    }

    @Test
    @DisplayName("Явный плейсхолдер prefix важнее префикса из конфига")
    void explicitPrefixPlaceholderWins() {

        StubConfig config = new StubConfig()
                .withPrefix("config")
                .withMessage(MessageKey.RELOAD, "{prefix}done");

        List<String> lines = service(config).build(MessageKey.RELOAD, Map.of("prefix", "custom"));

        assertEquals(List.of("customdone"), lines);

    }

    @Test
    @DisplayName("Префикс не пришивается сам: только через {prefix}")
    void prefixIsNotPrependedAutomatically() {

        StubConfig config = new StubConfig()
                .withPrefix("PFX")
                .withMessage(MessageKey.RELOAD, "no placeholder here");

        assertEquals(List.of("no placeholder here"),
                service(config).build(MessageKey.RELOAD, Map.of()));

    }

    @Test
    @DisplayName("Выключенное сообщение дает пустой список и ничего не шлет")
    void disabledMessageSendsNothing() {

        StubConfig config = new StubConfig();
        when(player.isOnline()).thenReturn(true);

        assertEquals(List.of(), service(config).build(MessageKey.USAGE, Map.of()));

        service(config).send(player, MessageKey.USAGE);

        verify(player, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());

    }

    @Test
    @DisplayName("Мастер-выключатель сообщений глушит доставку")
    void masterSwitchStopsDelivery() {

        StubConfig config = new StubConfig()
                .withMessagesEnabled(false)
                .withMessage(MessageKey.USAGE, "line");

        service(config).send(player, MessageKey.USAGE);

        verifyNoInteractions(player);

    }

    @Test
    @DisplayName("Оффлайн-игрок сообщения не получает")
    void offlinePlayerIsSkipped() {

        StubConfig config = new StubConfig().withMessage(MessageKey.USAGE, "line");
        when(player.isOnline()).thenReturn(false);

        service(config).send(player, MessageKey.USAGE);

        verify(player, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());

    }

    @Test
    @DisplayName("Null-получатель не роняет отправку")
    void nullSenderIsIgnored() {

        StubConfig config = new StubConfig().withMessage(MessageKey.USAGE, "line");

        service(config).send((CommandSender) null, MessageKey.USAGE);

        assertEquals(List.of("line"), service(config).build(MessageKey.USAGE, Map.of()));

    }

    @Test
    @DisplayName("Консоль получает сообщение без проверки онлайна")
    void consoleReceivesMessages() {

        StubConfig config = new StubConfig().withMessage(MessageKey.RELOAD, "reloaded {time}");

        service(config).send(console, MessageKey.RELOAD, Map.of("time", "3"));

        verify(console).sendMessage("reloaded 3");

    }

    @Test
    @DisplayName("Перегрузка без плейсхолдеров делегирует основной")
    void sendWithoutPlaceholdersDelegates() {

        StubConfig config = new StubConfig().withMessage(MessageKey.DISABLED, "parked");
        when(player.isOnline()).thenReturn(true);

        service(config).send(player, MessageKey.DISABLED);

        verify(player).sendMessage("parked");

    }

    @Test
    @DisplayName("Сообщение канала уходит по канальному ключу")
    void channelMessageIsDelivered() {

        StubConfig config = new StubConfig()
                .withChannelMessage(BroadcastChannel.AD, ChannelMessageKey.PAYMENT_SUCCESS,
                        "charged {count} for /{channel}");
        when(player.isOnline()).thenReturn(true);

        service(config).send(player, BroadcastChannel.AD, ChannelMessageKey.PAYMENT_SUCCESS,
                Map.of("count", "1500", "channel", "ad"));

        verify(player).sendMessage("charged 1500 for /ad");

    }

    @Test
    @DisplayName("Канальное сообщение собирается build() без отправки")
    void channelBuildReturnsLinesWithoutSending() {

        StubConfig config = new StubConfig()
                .withChannelMessage(BroadcastChannel.BC, ChannelMessageKey.USAGE,
                        "usage /{channel}", "second line");

        List<String> lines = service(config).build(BroadcastChannel.BC,
                ChannelMessageKey.USAGE, Map.of("channel", "bc"));

        assertEquals(List.of("usage /bc", "second line"), lines);
        verifyNoInteractions(player);

    }

    @Test
    @DisplayName("Канальное сообщение не уходит оффлайн-игроку")
    void offlinePlayerGetsNoChannelMessage() {

        StubConfig config = new StubConfig()
                .withChannelMessage(BroadcastChannel.BC, ChannelMessageKey.USAGE, "line");
        when(player.isOnline()).thenReturn(false);

        service(config).send(player, BroadcastChannel.BC, ChannelMessageKey.USAGE, Map.of());

        verify(player, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());

    }

    @Test
    @DisplayName("Незаданное канальное сообщение дает пустой список")
    void missingChannelMessageIsEmpty() {

        StubConfig config = new StubConfig();

        assertEquals(List.of(), service(config).build(BroadcastChannel.BUY,
                ChannelMessageKey.INSUFFICIENT_FUNDS, Map.of()));

    }

}
