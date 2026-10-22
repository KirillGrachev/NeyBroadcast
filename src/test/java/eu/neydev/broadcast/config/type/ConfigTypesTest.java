package eu.neydev.broadcast.config.type;

import org.bukkit.Sound;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Контракты перечислений конфигурации: уникальные ключи, непустые дефолты,
 * соответствие каналов командам и узлам прав, разбор имени команды.
 */
class ConfigTypesTest {

    @Test
    @DisplayName("Каналы уникальны, команда совпадает с ключом секции")
    void channelsAreUniqueAndMatchTheirCommands() {

        Set<String> keys = new HashSet<>();

        for (BroadcastChannel channel : BroadcastChannel.values()) {

            assertTrue(keys.add(channel.getConfigKey()), "дубль ключа " + channel.getConfigKey());
            assertEquals(channel.getConfigKey(), channel.getCommandName(),
                    "имя команды и ключ секции одного канала совпадают");
            assertNotNull(channel.getDefaultTemplate());
            assertFalse(channel.getDefaultTemplate().isEmpty(), "пустой шаблон у " + channel);

        }

        assertEquals(3, BroadcastChannel.values().length);

    }

    @Test
    @DisplayName("Стоимости каналов по умолчанию совпадают с поставляемым конфигом")
    void channelCostsMatchTheShippedFile() {

        assertEquals(0.0, BroadcastChannel.BC.getDefaultCost());
        assertEquals(1500.0, BroadcastChannel.AD.getDefaultCost());
        assertEquals(2000.0, BroadcastChannel.BUY.getDefaultCost());

    }

    @Test
    @DisplayName("Канал находится по имени команды без учета регистра")
    void channelIsFoundByCommandName() {

        assertSame(BroadcastChannel.BC, BroadcastChannel.fromCommand("bc"));
        assertSame(BroadcastChannel.BC, BroadcastChannel.fromCommand("BC"));
        assertSame(BroadcastChannel.AD, BroadcastChannel.fromCommand("ad"));
        assertSame(BroadcastChannel.BUY, BroadcastChannel.fromCommand("buy"));

    }

    @Test
    @DisplayName("Чужая команда и null канал не дают")
    void unknownCommandYieldsNoChannel() {

        assertNull(BroadcastChannel.fromCommand("gps"));
        assertNull(BroadcastChannel.fromCommand(""));
        assertNull(BroadcastChannel.fromCommand(null));

    }

    @Test
    @DisplayName("Ключи глобальных сообщений уникальны и имеют непустые дефолты")
    void messageKeysAreUnique() {

        Set<String> keys = new HashSet<>();

        for (MessageKey key : MessageKey.values()) {

            assertTrue(keys.add(key.getConfigKey()), "дубль ключа " + key.getConfigKey());
            assertFalse(key.getConfigKey().isBlank(), "пустой ключ у " + key);
            assertNotNull(key.getDefaultMessage(), "нет дефолта у " + key);
            assertFalse(key.getDefaultMessage().isBlank(), "пустой дефолт у " + key);
            assertTrue(key.isDefaultEnabled(), "сообщение выключено по умолчанию: " + key);

        }

        assertEquals(MessageKey.values().length, keys.size());

    }

    @Test
    @DisplayName("Ключи канальных сообщений уникальны и имеют непустые дефолты")
    void channelMessageKeysAreUnique() {

        Set<String> keys = new HashSet<>();

        for (ChannelMessageKey key : ChannelMessageKey.values()) {

            assertTrue(keys.add(key.getConfigKey()), "дубль ключа " + key.getConfigKey());
            assertFalse(key.getDefaultMessage().isBlank(), "пустой дефолт у " + key);
            assertTrue(key.isDefaultEnabled(), "сообщение выключено по умолчанию: " + key);

        }

        assertEquals(3, ChannelMessageKey.values().length);

    }

    @Test
    @DisplayName("Узлы прав уникальны, дефолты совпадают с plugin.yml")
    void permissionNodesMatchPluginYml() {

        Set<String> keys = new HashSet<>();

        for (PermissionNode node : PermissionNode.values()) {

            assertTrue(keys.add(node.getConfigKey()), "дубль ключа " + node.getConfigKey());
            assertFalse(node.getDefaultPermission().isBlank(), "пустое право у " + node);

        }

        assertEquals("ney_broadcast.bc", PermissionNode.BC.getDefaultPermission());
        assertEquals("ney_broadcast.ad", PermissionNode.AD.getDefaultPermission());
        assertEquals("ney_broadcast.buy", PermissionNode.BUY.getDefaultPermission());
        assertEquals("neybroadcast.reload", PermissionNode.RELOAD.getDefaultPermission());

    }

    @Test
    @DisplayName("Узлы каналов связаны со своими каналами, reload - служебный")
    void channelNodesCarryTheirChannel() {

        assertSame(BroadcastChannel.BC, PermissionNode.BC.getChannel());
        assertSame(BroadcastChannel.AD, PermissionNode.AD.getChannel());
        assertSame(BroadcastChannel.BUY, PermissionNode.BUY.getChannel());
        assertNull(PermissionNode.RELOAD.getChannel());

    }

    @Test
    @DisplayName("Поиск узла по каналу возвращает свой узел для каждого канала")
    void nodeIsFoundForEveryChannel() {

        for (BroadcastChannel channel : BroadcastChannel.values()) {
            assertSame(channel, PermissionNode.forChannel(channel).getChannel());
        }

    }

    @Test
    @DisplayName("Шаблоны консоли непустые, диагностические - со своими плейсхолдерами")
    void consoleTemplatesCarryPlaceholders() {

        for (ConsoleMessage message : ConsoleMessage.values()) {
            assertFalse(message.getDefaultTemplate().isBlank(), "пустой шаблон у " + message);
        }

        assertTrue(ConsoleMessage.INVALID_VALUE.getDefaultTemplate().contains("{path}"));
        assertTrue(ConsoleMessage.INVALID_VALUE.getDefaultTemplate().contains("{value}"));
        assertTrue(ConsoleMessage.INVALID_VALUE.getDefaultTemplate().contains("{defaultValue}"));
        assertTrue(ConsoleMessage.LEGACY_PATH.getDefaultTemplate().contains("{replacement}"));
        assertTrue(ConsoleMessage.COMMAND_MISSING.getDefaultTemplate().contains("{command}"));
        assertTrue(ConsoleMessage.COMMAND_UNREGISTER_FAILED.getDefaultTemplate().contains("{reason}"));

    }

    @Test
    @DisplayName("Звук играется только при включенной настройке и разрешенном имени")
    void soundIsPlayableOnlyWhenEnabledAndResolved() {

        assertTrue(new SoundSettings(true, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f).isPlayable());
        assertFalse(new SoundSettings(false, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f).isPlayable());
        assertFalse(new SoundSettings(true, null, 1.0f, 1.0f).isPlayable(),
                "нерезолвленное имя не играет");

    }

    @Test
    @DisplayName("Дефолты звука совпадают с поставляемым конфигом")
    void soundDefaultsMatchTheShippedFile() {

        assertTrue(SoundSettings.DEFAULT_ENABLED);
        assertEquals("BLOCK_NOTE_BLOCK_HAT", SoundSettings.DEFAULT_TYPE);
        assertEquals(0.7f, SoundSettings.DEFAULT_VOLUME);
        assertEquals(1.0f, SoundSettings.DEFAULT_PITCH);

    }

}
