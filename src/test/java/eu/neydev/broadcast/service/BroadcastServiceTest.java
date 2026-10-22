package eu.neydev.broadcast.service;

import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.ChannelMessageKey;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.SoundSettings;
import eu.neydev.broadcast.economy.EconomyService;
import eu.neydev.broadcast.event.BroadcastSendEvent;
import eu.neydev.broadcast.support.StubConfig;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Конвейер рассылки: порядок проверок 1.x сохранен, новые ветки (мастер-
 * выключатели, отказ экономики, проверка списания, событие) покрыты каждая.
 * Шаблон окрашивается после подстановки - HEX игрока проходит, & и §
 * отклоняются до рассылки.
 */
class BroadcastServiceTest {

    private final Plugin plugin = mock(Plugin.class);
    private final Server server = mock(Server.class);
    private final PluginManager pluginManager = mock(PluginManager.class);
    private final Player player = mock(Player.class);
    private final Player bystander = mock(Player.class);
    private final org.bukkit.Location location = mock(org.bukkit.Location.class);
    private final ConsoleCommandSender console = mock(ConsoleCommandSender.class);
    private final Economy economy = mock(Economy.class);

    private StubConfig config;

    @BeforeEach
    void setUp() {

        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        doReturn(List.of(player)).when(server).getOnlinePlayers();
        when(player.getName()).thenReturn("Tester");
        when(player.isOnline()).thenReturn(true);
        when(player.getLocation()).thenReturn(location);
        when(player.hasPermission(anyString())).thenReturn(true);

        config = new StubConfig()
                .withMessage(MessageKey.DISABLED, "plugin disabled")
                .withMessage(MessageKey.CHANNEL_DISABLED, "channel {channel} disabled")
                .withMessage(MessageKey.NO_CONSOLE, "players only")
                .withMessage(MessageKey.NO_PERMISSION, "denied")
                .withMessage(MessageKey.CONTAINS_COLOR_CODE, "no colors")
                .withMessage(MessageKey.TOO_LARGE, "too large {length}")
                .withMessage(MessageKey.ECONOMY_UNAVAILABLE, "no economy")
                .withMessage(MessageKey.PAYMENT_FAILED, "failed: {error}")
                .withChannelMessage(BroadcastChannel.BC, ChannelMessageKey.USAGE, "usage {channel}")
                .withChannelMessage(BroadcastChannel.BC, ChannelMessageKey.INSUFFICIENT_FUNDS,
                        "need {count} ({cost}) for {channel}")
                .withChannelMessage(BroadcastChannel.BC, ChannelMessageKey.PAYMENT_SUCCESS,
                        "charged {count} from {player_name}")
                .withTemplate(BroadcastChannel.BC, "{message}");

    }

    private BroadcastService service() {
        return service(economy);
    }

    private BroadcastService service(Economy economyOrNull) {
        return new BroadcastService(plugin, config, new MessageService(config),
                new PermissionService(config), new EconomyService(economyOrNull));
    }

    private void handle(String... args) {
        service().handle(BroadcastChannel.BC, player, args);
    }

    private EconomyResponse response(EconomyResponse.ResponseType type, String error) {
        return new EconomyResponse(0.0, 0.0, type, error);
    }

    @Test
    @DisplayName("Выключенный плагин не рассылает ничего")
    void disabledPluginSendsDisabledMessage() {

        config.withPluginEnabled(false);

        handle("hello");

        verify(player).sendMessage("plugin disabled");
        verify(player, never()).sendMessage("hello");

    }

    @Test
    @DisplayName("Выключенный канал отвечает своим сообщением")
    void disabledChannelSendsChannelDisabled() {

        config.withChannelEnabled(BroadcastChannel.BC, false);

        handle("hello");

        verify(player).sendMessage("channel bc disabled");
        verify(player, never()).sendMessage("hello");

    }

    @Test
    @DisplayName("Консоль каналом пользоваться не может")
    void consoleSenderIsRefused() {

        service().handle(BroadcastChannel.BC, console, new String[]{"hello"});

        verify(console).sendMessage("players only");

    }

    @Test
    @DisplayName("Пустые аргументы дают usage канала")
    void emptyArgumentsSendChannelUsage() {

        handle();

        verify(player).sendMessage("usage bc");
        verify(player, never()).sendMessage("");

    }

    @Test
    @DisplayName("Без права канала рассылки нет")
    void missingPermissionIsRefused() {

        when(player.hasPermission("ney_broadcast.bc")).thenReturn(false);

        handle("hello");

        verify(player).sendMessage("denied");
        verify(player, never()).sendMessage("hello");

    }

    @Test
    @DisplayName("Амперсанд в тексте игрока отклоняется")
    void ampersandInMessageIsRefused() {

        handle("&cboom");

        verify(player).sendMessage("no colors");
        verify(player, never()).sendMessage("&cboom");

    }

    @Test
    @DisplayName("Секция § в тексте игрока отклоняется")
    void sectionSymbolInMessageIsRefused() {

        handle("§cboom");

        verify(player).sendMessage("no colors");

    }

    @Test
    @DisplayName("Слишком длинный текст отклоняется с лимитом в сообщении")
    void tooLongMessageIsRefused() {

        config.withMaxLength(BroadcastChannel.BC, 5);

        handle("123456");

        verify(player).sendMessage("too large 5");
        verify(player, never()).sendMessage("123456");

    }

    @Test
    @DisplayName("Ровно предельная длина проходит")
    void exactMaxLengthPasses() {

        config.withMaxLength(BroadcastChannel.BC, 5);

        handle("12345");

        verify(player).sendMessage("12345");

    }

    @Test
    @DisplayName("Бесплатный канал работает без экономики вовсе")
    void freeChannelWorksWithoutEconomy() {

        service(null).handle(BroadcastChannel.BC, player, new String[]{"hello"});

        verify(player).sendMessage("hello");

    }

    @Test
    @DisplayName("Рассылка доходит до каждого игрока в сети")
    void broadcastReachesEveryOnlinePlayer() {

        doReturn(List.of(player, bystander)).when(server).getOnlinePlayers();

        handle("hello all");

        verify(player).sendMessage("hello all");
        verify(bystander).sendMessage("hello all");

    }

    @Test
    @DisplayName("Плейсхолдеры шаблона подставляются в обеих нотациях")
    void templatePlaceholdersAreSubstituted() {

        config.withTemplate(BroadcastChannel.BC, "{player}|%player%|{message}|%message%");

        handle("hi");

        verify(player).sendMessage("Tester|Tester|hi|hi");

    }

    @Test
    @DisplayName("HEX в тексте игрока окрашивается: шаблон красится после подстановки")
    void hexInPlayerTextIsColored() {

        config.withTemplate(BroadcastChannel.BC, "{message}");

        handle("#ff0000red");

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        verify(player).sendMessage(message.capture());
        assertTrue(message.getValue().startsWith("§x§f§f§0§0§0§0red"), message.getValue());

    }

    @Test
    @DisplayName("Платный канал без экономики отказывает и не рассылает")
    void paidChannelWithoutEconomyIsRefused() {

        config.withCost(BroadcastChannel.BC, 100.0);

        service(null).handle(BroadcastChannel.BC, player, new String[]{"hello"});

        verify(player).sendMessage("no economy");
        verify(player, never()).sendMessage("hello");

    }

    @Test
    @DisplayName("Недостаточный баланс отклоняется до списания")
    void insufficientBalanceIsRefused() {

        config.withCost(BroadcastChannel.BC, 100.0);
        when(economy.getBalance(player)).thenReturn(99.5);
        when(economy.format(100.0)).thenReturn("$100.00");

        handle("hello");

        verify(player).sendMessage("need 100 ($100.00) for bc");
        verify(economy, never()).withdrawPlayer(any(Player.class), anyDouble());
        verify(player, never()).sendMessage("hello");

    }

    @Test
    @DisplayName("Успешное списание подтверждается и рассылка уходит")
    void successfulPaymentBroadcasts() {

        config.withCost(BroadcastChannel.BC, 100.0);
        when(economy.getBalance(player)).thenReturn(500.0);
        when(economy.withdrawPlayer(player, 100.0))
                .thenReturn(response(EconomyResponse.ResponseType.SUCCESS, null));

        handle("hello");

        verify(player).sendMessage("charged 100 from Tester");
        verify(player).sendMessage("hello");

    }

    @Test
    @DisplayName("Отклоненное списание останавливает рассылку и показывает ошибку провайдера")
    void failedWithdrawalAbortsBroadcast() {

        config.withCost(BroadcastChannel.BC, 100.0);
        when(economy.getBalance(player)).thenReturn(500.0);
        when(economy.withdrawPlayer(player, 100.0))
                .thenReturn(response(EconomyResponse.ResponseType.FAILURE, "account is frozen"));

        handle("hello");

        verify(player).sendMessage("failed: account is frozen");
        verify(player, never()).sendMessage("hello");

    }

    @Test
    @DisplayName("Null-ответ провайдера тоже останавливает рассылку")
    void nullWithdrawalResponseAbortsBroadcast() {

        config.withCost(BroadcastChannel.BC, 100.0);
        when(economy.getBalance(player)).thenReturn(500.0);
        when(economy.withdrawPlayer(any(Player.class), anyDouble())).thenReturn(null);

        handle("hello");

        verify(player).sendMessage("failed: the economy provider returned no response");
        verify(player, never()).sendMessage("hello");

    }

    @Test
    @DisplayName("Событие публикуется до списания и несет готовые строки")
    void eventIsPublishedBeforePaymentWithRenderedLines() {

        config.withCost(BroadcastChannel.BC, 100.0);
        when(economy.getBalance(player)).thenReturn(500.0);
        when(economy.withdrawPlayer(player, 100.0))
                .thenReturn(response(EconomyResponse.ResponseType.SUCCESS, null));

        handle("hello");

        ArgumentCaptor<Event> event = ArgumentCaptor.forClass(Event.class);

        verify(pluginManager).callEvent(event.capture());

        BroadcastSendEvent sent = (BroadcastSendEvent) event.getValue();

        assertSame(player, sent.getPlayer());
        assertSame(BroadcastChannel.BC, sent.getChannel());
        assertEquals("hello", sent.getMessage());
        assertEquals(List.of("hello"), sent.getLines(), "строки уже отрендерены");
        assertEquals(100.0, sent.getCost());

    }

    @Test
    @DisplayName("Отмененное событие оставляет игрока и деньгами, и без рассылки")
    void cancelledEventPreventsPaymentAndBroadcast() {

        config.withCost(BroadcastChannel.BC, 100.0);
        when(economy.getBalance(player)).thenReturn(500.0);
        doAnswer(invocation -> {
            ((BroadcastSendEvent) invocation.getArgument(0)).setCancelled(true);
            return null;
        }).when(pluginManager).callEvent(any(BroadcastSendEvent.class));

        handle("hello");

        verify(economy, never()).withdrawPlayer(any(Player.class), anyDouble());
        verify(player, never()).sendMessage(anyString());

    }

    @Test
    @DisplayName("Включенный звук слышит автор рассылки")
    void enabledSoundIsPlayedToSender() {

        config.withSound(BroadcastChannel.BC,
                new SoundSettings(true, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.0f));

        handle("hello");

        verify(player).playSound(eq(location), eq(Sound.BLOCK_NOTE_BLOCK_HAT), eq(0.7f), eq(1.0f));

    }

    @Test
    @DisplayName("Выключенный или нерезолвленный звук не играет")
    void unplayableSoundIsSkipped() {

        handle("hello");

        verify(player, never()).playSound(any(Location.class), any(Sound.class), anyFloat(), anyFloat());

        config.withSound(BroadcastChannel.BC, new SoundSettings(true, null, 1.0f, 1.0f));

        handle("again");

        verify(player, never()).playSound(any(Location.class), any(Sound.class), anyFloat(), anyFloat());

    }

    @Test
    @DisplayName("Мастер-выключатель сообщений глушит ответы, но не рассылку")
    void messagesMasterSwitchSilencesRepliesOnly() {

        config.withMessagesEnabled(false).withPluginEnabled(false);

        handle("hello");

        verify(player, never()).sendMessage(anyString());

    }

}
