package eu.neydev.broadcast.command.sub;

import eu.neydev.broadcast.command.CommandContext;
import eu.neydev.broadcast.config.ConfigManager;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.service.MessageService;
import eu.neydev.broadcast.support.StubConfig;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Подкоманда reload: конфигурация действительно перечитывается, а отправитель
 * получает подтверждение со временем и числом включенных каналов.
 */
class ReloadSubCommandTest {

    private final ConfigManager configManager = mock(ConfigManager.class);
    private final CommandSender sender = mock(CommandSender.class);
    private final StubConfig config = new StubConfig()
            .withMessage(MessageKey.RELOAD, "reloaded in {time} ms, {channels} of {total} channels");

    private ReloadSubCommand subCommand() {
        return new ReloadSubCommand(configManager, new MessageService(config));
    }

    @Test
    @DisplayName("Название и узел права совпадают с plugin.yml")
    void nameAndPermissionMatchPluginYml() {

        assertEquals("reload", subCommand().getName());
        assertEquals(PermissionNode.RELOAD, subCommand().getPermission());

    }

    @Test
    @DisplayName("Конфигурация перечитывается, отправитель получает время и счет каналов")
    void reloadsConfigAndReportsElapsed() {

        when(configManager.isChannelEnabled(BroadcastChannel.BC)).thenReturn(true);
        when(configManager.isChannelEnabled(BroadcastChannel.AD)).thenReturn(true);
        when(configManager.isChannelEnabled(BroadcastChannel.BUY)).thenReturn(false);

        assertTrue(subCommand().execute(CommandContext.of(sender), new String[0]));

        verify(configManager).reload();

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        verify(sender).sendMessage(message.capture());
        assertTrue(message.getValue().matches("reloaded in \\d+ ms, 2 of 3 channels"),
                message.getValue());

    }

    @Test
    @DisplayName("Аргументы после названия подкоманды игнорируются")
    void extraArgumentsAreIgnored() {

        assertTrue(subCommand().execute(CommandContext.of(sender), new String[]{"now", "please"}));

        verify(configManager).reload();

    }

    @Test
    @DisplayName("Автодополнения у reload нет")
    void completionIsEmpty() {
        assertEquals(List.of(), subCommand().complete(CommandContext.of(sender), new String[0]));
    }

}
