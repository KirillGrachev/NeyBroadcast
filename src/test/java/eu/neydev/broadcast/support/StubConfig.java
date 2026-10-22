package eu.neydev.broadcast.support;

import eu.neydev.broadcast.config.BroadcastConfig;
import eu.neydev.broadcast.config.type.BroadcastChannel;
import eu.neydev.broadcast.config.type.ChannelMessageKey;
import eu.neydev.broadcast.config.type.MessageKey;
import eu.neydev.broadcast.config.type.PermissionNode;
import eu.neydev.broadcast.config.type.SoundSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Изменяемый стаб конфигурации для юнит-тестов.
 *
 * Сервисы и команды проверяются без Bukkit и без YAML: тест задает ровно те
 * значения, которые нужны сценарию, и не зависит от порядка ключей в
 * config.yml. Дефолты повторяют поставляемый файл (стоимости каналов,
 * лимит 90, шаблоны), кроме звука: он по умолчанию выключен, чтобы тесты
 * рассылки не обязаны были готовить playSound на каждом моке игрока.
 */
public final class StubConfig implements BroadcastConfig {

    private boolean pluginEnabled = true;
    private List<String> suggestions = List.of("Hello everyone!");
    private boolean permissionsEnabled = true;
    private boolean messagesEnabled = true;
    private String prefix = "";
    private final Map<PermissionNode, String> permissions = new EnumMap<>(PermissionNode.class);
    private final Map<MessageKey, List<String>> messages = new EnumMap<>(MessageKey.class);
    private final Map<BroadcastChannel, ChannelStub> channels = new EnumMap<>(BroadcastChannel.class);

    private static final class ChannelStub {

        private boolean enabled = true;
        private int maxLength = 90;
        private Double cost;
        private List<String> template;
        private SoundSettings sound = new SoundSettings(false, null, 1.0f, 1.0f);
        private final Map<ChannelMessageKey, List<String>> messages =
                new EnumMap<>(ChannelMessageKey.class);

    }

    private @NotNull ChannelStub channel(@NotNull BroadcastChannel channel) {
        return channels.computeIfAbsent(channel, key -> new ChannelStub());
    }

    public @NotNull StubConfig withPluginEnabled(boolean enabled) {
        this.pluginEnabled = enabled;
        return this;
    }

    public @NotNull StubConfig withSuggestions(@NotNull String... values) {
        this.suggestions = Arrays.asList(values);
        return this;
    }

    public @NotNull StubConfig withPermissionsEnabled(boolean enabled) {
        this.permissionsEnabled = enabled;
        return this;
    }

    public @NotNull StubConfig withMessagesEnabled(boolean enabled) {
        this.messagesEnabled = enabled;
        return this;
    }

    public @NotNull StubConfig withPrefix(@NotNull String value) {
        this.prefix = value;
        return this;
    }

    public @NotNull StubConfig withPermission(@NotNull PermissionNode node, @NotNull String value) {
        permissions.put(node, value);
        return this;
    }

    public @NotNull StubConfig withMessage(@NotNull MessageKey key, @NotNull String... lines) {
        messages.put(key, Arrays.asList(lines));
        return this;
    }

    public @NotNull StubConfig withChannelEnabled(@NotNull BroadcastChannel channel, boolean enabled) {
        channel(channel).enabled = enabled;
        return this;
    }

    public @NotNull StubConfig withMaxLength(@NotNull BroadcastChannel channel, int maxLength) {
        channel(channel).maxLength = maxLength;
        return this;
    }

    public @NotNull StubConfig withCost(@NotNull BroadcastChannel channel, double cost) {
        channel(channel).cost = cost;
        return this;
    }

    public @NotNull StubConfig withTemplate(@NotNull BroadcastChannel channel, @NotNull String... lines) {
        channel(channel).template = Arrays.asList(lines);
        return this;
    }

    public @NotNull StubConfig withSound(@NotNull BroadcastChannel channel, @NotNull SoundSettings sound) {
        channel(channel).sound = sound;
        return this;
    }

    public @NotNull StubConfig withChannelMessage(@NotNull BroadcastChannel channel,
                                                  @NotNull ChannelMessageKey key,
                                                  @NotNull String... lines) {
        channel(channel).messages.put(key, Arrays.asList(lines));
        return this;
    }

    @Override
    public boolean isPluginEnabled() {
        return pluginEnabled;
    }

    @Override
    public @NotNull List<String> getSuggestions() {
        return suggestions;
    }

    @Override
    public boolean arePermissionsEnabled() {
        return permissionsEnabled;
    }

    @Override
    public @NotNull String getPermission(@NotNull PermissionNode node) {
        return permissions.getOrDefault(node, node.getDefaultPermission());
    }

    @Override
    public boolean areMessagesEnabled() {
        return messagesEnabled;
    }

    @Override
    public @NotNull String getMessagePrefix() {
        return prefix;
    }

    @Override
    public @NotNull List<String> getMessages(@NotNull MessageKey key) {
        return messages.getOrDefault(key, List.of());
    }

    @Override
    public @NotNull List<String> getMessages(@NotNull BroadcastChannel channel,
                                             @NotNull ChannelMessageKey key) {
        return channel(channel).messages.getOrDefault(key, List.of());
    }

    @Override
    public boolean isChannelEnabled(@NotNull BroadcastChannel channel) {
        return channel(channel).enabled;
    }

    @Override
    public int getMaxLength(@NotNull BroadcastChannel channel) {
        return channel(channel).maxLength;
    }

    @Override
    public double getCost(@NotNull BroadcastChannel channel) {
        Double cost = channel(channel).cost;
        return cost == null ? channel.getDefaultCost() : cost;
    }

    @Override
    public @NotNull List<String> getTemplate(@NotNull BroadcastChannel channel) {
        List<String> template = channel(channel).template;
        return template == null ? channel.getDefaultTemplate() : template;
    }

    @Override
    public @NotNull SoundSettings getSound(@NotNull BroadcastChannel channel) {
        return channel(channel).sound;
    }

}
