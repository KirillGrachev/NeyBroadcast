package eu.neydev.broadcast.command.sub;

import eu.neydev.broadcast.command.CommandContext;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.economy.EconomyService;
import eu.neydev.broadcast.service.MessageService;
import eu.neydev.broadcast.support.StubConfig;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Подкоманда info: диагностика собирается из снапшота конфигурации и живого
 * состояния экономики, булевы значения подставляются словами из той же
 * конфигурации.
 */
class InfoSubCommandTest {

    private static final String TEMPLATE =
            "v={version} c={channels} e={economy} p={provider} perm={permissions}";

    private final Plugin plugin = mock(Plugin.class);
    private final CommandSender sender = mock(CommandSender.class);
    private final StubConfig config = new StubConfig()
            .withMessage(MessageKey.INFO, TEMPLATE)
            .withMessage(MessageKey.STATE_ON, "on")
            .withMessage(MessageKey.STATE_OFF, "off");

    @BeforeEach
    void setUp() {
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile(
                "NeyBroadcast", "2.0.0", "eu.neydev.broadcast.NeyBroadcast"));
    }

    private InfoSubCommand subCommand(EconomyService economyService) {
        return new InfoSubCommand(plugin, config, economyService, new MessageService(config));
    }

    private String sent() {

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        verify(sender).sendMessage(message.capture());
        return message.getValue();

    }

    @Test
    @DisplayName("Название и узел права совпадают с plugin.yml")
    void nameAndPermissionMatchPluginYml() {

        InfoSubCommand subCommand = subCommand(new EconomyService(null));

        assertEquals("info", subCommand.getName());
        assertEquals(PermissionNode.RELOAD, subCommand.getPermission());

    }

    @Test
    @DisplayName("Без экономики диагностика честная: выключена, провайдер none")
    void reportsStateWithoutEconomy() {

        assertTrue(subCommand(new EconomyService(null))
                .execute(CommandContext.of(sender), new String[0]));

        assertEquals("v=2.0.0 c=bc, ad, buy e=off p=none perm=on", sent());

    }

    @Test
    @DisplayName("Подключенная экономика печатается словом state_on и именем провайдера")
    void reportsConnectedEconomy() {

        Economy economy = mock(Economy.class);
        when(economy.getName()).thenReturn("Essentials Economy");

        subCommand(new EconomyService(economy)).execute(CommandContext.of(sender), new String[0]);

        assertEquals("v=2.0.0 c=bc, ad, buy e=on p=Essentials Economy perm=on", sent());

    }

    @Test
    @DisplayName("Выключенные каналы не попадают в список")
    void disabledChannelsAreExcluded() {

        config.withChannelEnabled(BroadcastChannel.AD, false)
                .withChannelEnabled(BroadcastChannel.BUY, false);

        subCommand(new EconomyService(null)).execute(CommandContext.of(sender), new String[0]);

        assertTrue(sent().contains("c=bc "), sent());

    }

    @Test
    @DisplayName("Все каналы выключены - печатается none")
    void noEnabledChannelsPrintsNone() {

        config.withChannelEnabled(BroadcastChannel.BC, false)
                .withChannelEnabled(BroadcastChannel.AD, false)
                .withChannelEnabled(BroadcastChannel.BUY, false);

        subCommand(new EconomyService(null)).execute(CommandContext.of(sender), new String[0]);

        assertTrue(sent().contains("c=none"), sent());

    }

    @Test
    @DisplayName("Выключенная система прав печатается словом state_off")
    void disabledPermissionsUseStateOff() {

        config.withPermissionsEnabled(false);

        subCommand(new EconomyService(null)).execute(CommandContext.of(sender), new String[0]);

        assertTrue(sent().contains("perm=off"), sent());

    }

    @Test
    @DisplayName("Выключенное слово-фрагмент дает пустую подстановку")
    void disabledStateFragmentBecomesEmpty() {

        config.withMessage(MessageKey.STATE_OFF);

        subCommand(new EconomyService(null)).execute(CommandContext.of(sender), new String[0]);

        assertTrue(sent().contains("e= "), sent());

    }

}
