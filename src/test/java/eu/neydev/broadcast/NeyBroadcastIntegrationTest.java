package eu.neydev.broadcast;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.event.BroadcastSendEvent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Интеграционные тесты на MockBukkit: плагин загружается в живой серверный
 * мок, команды исполняются через command map, событие рассылки ходит через
 * настоящий диспетчер событий, конфигурация сохраняется в папку плагина.
 *
 * Главный контракт 2.0 проверяется здесь же: отсутствие Vault больше не
 * мешает загрузке - бесплатный канал работает, платный вежливо отказывает.
 */
class NeyBroadcastIntegrationTest {

    private ServerMock server;
    private NeyBroadcast plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(NeyBroadcast.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private List<String> drainMessages(PlayerMock player, int count) {

        List<String> messages = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            messages.add(String.valueOf(player.nextMessage()));
        }

        return messages;

    }

    @Test
    @DisplayName("Плагин стартует без Vault: экономики нет, каналы живы")
    void pluginStartsWithoutVault() {

        assertTrue(plugin.isEnabled());
        assertNotNull(plugin.getServices());
        assertNotNull(plugin.getConfigManager());
        assertNotNull(plugin.getConsoleService());
        assertFalse(plugin.getEconomyService().isAvailable(),
                "экономики нет - плагин все равно работает");

    }

    @Test
    @DisplayName("Все четыре команды получают executor и tab-completer")
    void everyCommandIsWired() {

        for (String name : List.of("bc", "ad", "buy", "neybroadcast")) {

            PluginCommand command = plugin.getCommand(name);

            assertNotNull(command, "команда " + name + " найдена в plugin.yml");
            assertNotNull(command.getExecutor(), "executor привязан у " + name);
            assertNotNull(command.getTabCompleter(), "tab-completer привязан у " + name);

        }

    }

    @Test
    @DisplayName("Права из plugin.yml зарегистрированы на сервере")
    void permissionsFromPluginYmlAreRegistered() {

        assertNotNull(server.getPluginManager().getPermission("ney_broadcast.bc"));
        assertNotNull(server.getPluginManager().getPermission("ney_broadcast.ad"));
        assertNotNull(server.getPluginManager().getPermission("ney_broadcast.buy"));
        assertNotNull(server.getPluginManager().getPermission("neybroadcast.reload"));
        assertNotNull(server.getPluginManager().getPermission("neybroadcast.admin"));

    }

    @Test
    @DisplayName("Поставляемый config.yml читается в документированные значения")
    void shippedConfigCarriesDocumentedDefaults() {

        assertTrue(plugin.getDataFolder().toPath().resolve("config.yml").toFile().exists(),
                "config.yml сохранен в папку плагина");
        assertEquals(0.0, plugin.getConfigManager().getCost(BroadcastChannel.BC));
        assertEquals(1500.0, plugin.getConfigManager().getCost(BroadcastChannel.AD));
        assertEquals(2000.0, plugin.getConfigManager().getCost(BroadcastChannel.BUY));
        assertEquals(90, plugin.getConfigManager().getMaxLength(BroadcastChannel.BC));
        assertEquals(3, plugin.getConfigManager().getSuggestions().size());
        assertTrue(plugin.getConfigManager().arePermissionsEnabled());

    }

    @Test
    @DisplayName("Бесплатный канал рассылает текст без экономики")
    void freeChannelBroadcastsWithoutEconomy() {

        PlayerMock player = server.addPlayer("Steve");

        assertTrue(player.performCommand("bc hello world"));

        List<String> messages = drainMessages(player, 4);
        String joined = String.join("\n", messages);

        assertTrue(joined.contains("ANNOUNCEMENT"), joined);
        assertTrue(joined.contains("Steve"), joined);
        assertTrue(joined.contains("hello world"), joined);

    }

    @Test
    @DisplayName("Платный канал без экономики отказывает с объяснением")
    void paidChannelRefusesWithoutEconomy() {

        PlayerMock player = server.addPlayer("Steve");

        player.performCommand("ad sell my stuff");

        assertTrue(String.valueOf(player.nextMessage()).contains("No economy provider"),
                "игрок понял, почему отказ");

    }

    @Test
    @DisplayName("С зарегистрированной экономикой платный канал списывает и рассылает")
    void paidChannelWorksWithRegisteredEconomy() {

        MockBukkit.unmock();
        server = MockBukkit.mock();

        Economy economy = mock(Economy.class);
        Plugin registrant = mock(Plugin.class);

        when(economy.getName()).thenReturn("MockEconomy");
        when(economy.getBalance(any(PlayerMock.class))).thenReturn(5000.0);
        when(economy.withdrawPlayer(any(PlayerMock.class), anyDouble())).thenReturn(
                new EconomyResponse(1500.0, 3500.0, EconomyResponse.ResponseType.SUCCESS, null));
        when(economy.format(1500.0)).thenReturn("$1,500.00");

        server.getServicesManager().register(Economy.class, economy, registrant,
                ServicePriority.Normal);

        plugin = MockBukkit.load(NeyBroadcast.class);

        assertTrue(plugin.getEconomyService().isAvailable());

        PlayerMock player = server.addPlayer("Steve");

        player.performCommand("ad sell my stuff");

        List<String> messages = drainMessages(player, 5);
        String joined = String.join("\n", messages);

        assertTrue(joined.contains("Charged 1500 coins"), joined);
        assertTrue(joined.contains("ADVERTISEMENT"), joined);
        assertTrue(joined.contains("sell my stuff"), joined);
        verify(economy).withdrawPlayer(player, 1500.0);

    }

    @Test
    @DisplayName("Отмененное событие глушит рассылку через настоящий диспетчер")
    void cancelledEventSilencesBroadcast() {

        server.getPluginManager().registerEvents(new Listener() {

            @org.bukkit.event.EventHandler
            public void onBroadcast(BroadcastSendEvent event) {
                event.setCancelled(true);
            }

        }, plugin);

        PlayerMock player = server.addPlayer("Steve");

        player.performCommand("bc silent text");

        assertNull(player.nextMessage(), "ни одна строка шаблона не ушла");

    }

    @Test
    @DisplayName("Команда reload исполняется через command map")
    void reloadCommandRunsThroughCommandMap() {

        PlayerMock player = server.addPlayer("Steve");
        player.setOp(true);

        assertTrue(player.performCommand("neybroadcast reload"));
        assertTrue(String.valueOf(player.nextMessage()).contains("Configuration reloaded"));

    }

    @Test
    @DisplayName("Команда info печатает состояние из снапшота")
    void infoCommandReportsTheState() {

        PlayerMock player = server.addPlayer("Steve");
        player.setOp(true);

        player.performCommand("neybroadcast info");

        List<String> lines = drainMessages(player, 4);

        assertTrue(lines.get(0).contains("Version") && lines.get(0).contains("2.0.0"),
                lines.get(0));
        assertTrue(lines.get(1).contains("bc, ad, buy"), lines.get(1));
        assertTrue(lines.get(2).contains("none"), lines.get(2));
        assertTrue(lines.get(3).contains("Permissions"), lines.get(3));

    }

    @Test
    @DisplayName("Без аргументов служебная команда печатает справку")
    void controlCommandWithoutArgumentsPrintsUsage() {

        PlayerMock player = server.addPlayer("Steve");

        player.performCommand("neybroadcast");

        assertTrue(String.valueOf(player.nextMessage()).contains("NeyBroadcast commands:"));

    }

    @Test
    @DisplayName("Канал без текста печатает свой usage")
    void channelWithoutArgumentsPrintsUsage() {

        PlayerMock player = server.addPlayer("Steve");

        player.performCommand("bc");

        assertTrue(String.valueOf(player.nextMessage()).contains("Usage: /bc"));

    }

    @Test
    @DisplayName("Без права reload недоступен даже в списке автодополнения")
    void reloadIsDeniedForNonOperators() {

        PlayerMock player = server.addPlayer("Steve");

        player.performCommand("neybroadcast reload");

        assertTrue(String.valueOf(player.nextMessage()).contains("permission"));

        PluginCommand command = plugin.getCommand("neybroadcast");

        assertNotNull(command);
        assertEquals(List.of(), command.getTabCompleter().onTabComplete(player, command,
                "neybroadcast", new String[]{""}), "reload скрыт из дополнения");

    }

    @Test
    @DisplayName("Автодополнение канала предлагает варианты из конфига")
    void channelTabCompletionUsesConfiguredSuggestions() {

        PlayerMock player = server.addPlayer("Steve");
        PluginCommand command = plugin.getCommand("bc");

        assertNotNull(command);
        assertEquals(List.of("Hello everyone!"),
                command.getTabCompleter().onTabComplete(player, command, "bc",
                        new String[]{"Hello"}));

    }

    @Test
    @DisplayName("Отключение плагина проходит без ошибок и снимает команды")
    void pluginStopsGracefully() {

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(plugin.isEnabled());

    }

}
