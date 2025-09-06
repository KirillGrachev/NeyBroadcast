package com.ney.broadcast.config;

import com.ney.broadcast.util.HexColorUtil;
import com.ney.broadcast.NeyBroadcast;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {

    private final NeyBroadcast plugin;
    private FileConfiguration config;

    private static final String PATH_ENABLED = "enabled";
    private static final String PATH_MSG_NO_CONSOLE = "messages.no_console";
    private static final String PATH_MSG_NO_PERMISSION = "messages.no_permission";
    private static final String PATH_MSG_COLOR_CODE = "messages.contains_color_code";

    private static final String PATH_BC_PERMISSION = "commands.bc.permission";
    private static final String PATH_BC_MAX_LENGTH = "commands.bc.max_length";
    private static final String PATH_BC_COST = "commands.bc.cost";
    private static final String PATH_BC_TEMPLATE = "commands.bc.broadcast_template";
    private static final String PATH_BC_SOUND_ENABLED = "commands.bc.sound.enabled";
    private static final String PATH_BC_SOUND_TYPE = "commands.bc.sound.type";
    private static final String PATH_BC_SOUND_VOLUME = "commands.bc.sound.volume";
    private static final String PATH_BC_SOUND_PITCH = "commands.bc.sound.pitch";
    private static final String PATH_BC_MSG_USAGE = "commands.bc.messages.usage";
    private static final String PATH_BC_MSG_INSUFFICIENT = "commands.bc.messages.insufficient_funds";
    private static final String PATH_BC_MSG_SUCCESS = "commands.bc.messages.payment_success";

    private static final String PATH_AD_PERMISSION = "commands.ad.permission";
    private static final String PATH_AD_MAX_LENGTH = "commands.ad.max_length";
    private static final String PATH_AD_COST = "commands.ad.cost";
    private static final String PATH_AD_TEMPLATE = "commands.ad.broadcast_template";
    private static final String PATH_AD_SOUND_ENABLED = "commands.ad.sound.enabled";
    private static final String PATH_AD_SOUND_TYPE = "commands.ad.sound.type";
    private static final String PATH_AD_SOUND_VOLUME = "commands.ad.sound.volume";
    private static final String PATH_AD_SOUND_PITCH = "commands.ad.sound.pitch";
    private static final String PATH_AD_MSG_USAGE = "commands.ad.messages.usage";
    private static final String PATH_AD_MSG_INSUFFICIENT = "commands.ad.messages.insufficient_funds";
    private static final String PATH_AD_MSG_SUCCESS = "commands.ad.messages.payment_success";

    private static final String PATH_BUY_PERMISSION = "commands.buy.permission";
    private static final String PATH_BUY_MAX_LENGTH = "commands.buy.max_length";
    private static final String PATH_BUY_COST = "commands.buy.cost";
    private static final String PATH_BUY_TEMPLATE = "commands.buy.broadcast_template";
    private static final String PATH_BUY_SOUND_ENABLED = "commands.buy.sound.enabled";
    private static final String PATH_BUY_SOUND_TYPE = "commands.buy.sound.type";
    private static final String PATH_BUY_SOUND_VOLUME = "commands.buy.sound.volume";
    private static final String PATH_BUY_SOUND_PITCH = "commands.buy.sound.pitch";
    private static final String PATH_BUY_MSG_USAGE = "commands.buy.messages.usage";
    private static final String PATH_BUY_MSG_INSUFFICIENT = "commands.buy.messages.insufficient_funds";
    private static final String PATH_BUY_MSG_SUCCESS = "commands.buy.messages.payment_success";

    private boolean enabled;
    private String msgNoConsole, msgNoPermission, msgColorCode;

    private double bcCost, adCost, buyCost;
    private int bcSize, adSize, buySize;
    private List<String> bcTemplate, adTemplate, buyTemplate;
    private boolean bcSoundEnabled, adSoundEnabled, buySoundEnabled;
    private String bcSoundType, adSoundType, buySoundType;
    private float bcSoundVolume, adSoundVolume, buySoundVolume;
    private float bcSoundPitch, adSoundPitch, buySoundPitch;

    private String bcPermission, adPermission, buyPermission;
    private String bcUsage, adUsage, buyUsage;
    private String bcNoMoney, adNoMoney, buyNoMoney;
    private String bcMoneySuccess, adMoneySuccess, buyMoneySuccess;

    public ConfigManager(@NotNull NeyBroadcast plugin) {

        this.plugin = plugin;
        saveDefaultConfig();

        loadConfig();
        cacheConfigValues();

    }

    private void saveDefaultConfig() {
        plugin.saveDefaultConfig();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void cacheConfigValues() {

        enabled = config.getBoolean(PATH_ENABLED, true);

        msgNoConsole = HexColorUtil.color(config.getString(PATH_MSG_NO_CONSOLE, "&cТолько для игроков!"));
        msgNoPermission = HexColorUtil.color(config.getString(PATH_MSG_NO_PERMISSION, "&cНет прав!"));
        msgColorCode = HexColorUtil.color(config.getString(PATH_MSG_COLOR_CODE, "&cЗапрещены символы & и §"));

        bcPermission = config.getString(PATH_BC_PERMISSION, "ney_broadcast.bc");
        bcSize = config.getInt(PATH_BC_MAX_LENGTH, 90);
        bcCost = config.getDouble(PATH_BC_COST, 0.0);
        bcTemplate = getStringListWithColor(PATH_BC_TEMPLATE);
        bcSoundEnabled = config.getBoolean(PATH_BC_SOUND_ENABLED, false);
        bcSoundType = config.getString(PATH_BC_SOUND_TYPE, "BLOCK_NOTE_BLOCK_PLING");
        bcSoundVolume = (float) config.getDouble(PATH_BC_SOUND_VOLUME, 1.0);
        bcSoundPitch = (float) config.getDouble(PATH_BC_SOUND_PITCH, 1.0);
        bcUsage = HexColorUtil.color(config.getString(PATH_BC_MSG_USAGE, "&eИспользуйте /bc [текст]"));
        bcNoMoney = HexColorUtil.color(config.getString(PATH_BC_MSG_INSUFFICIENT, "&cНедостаточно средств!"));
        bcMoneySuccess = HexColorUtil.color(config.getString(PATH_BC_MSG_SUCCESS, "&aСписано {count} монет!"));

        adPermission = config.getString(PATH_AD_PERMISSION, "ney_broadcast.ad");
        adSize = config.getInt(PATH_AD_MAX_LENGTH, 90);
        adCost = config.getDouble(PATH_AD_COST, 1500.0);
        adTemplate = getStringListWithColor(PATH_AD_TEMPLATE);
        adSoundEnabled = config.getBoolean(PATH_AD_SOUND_ENABLED, false);
        adSoundType = config.getString(PATH_AD_SOUND_TYPE, "BLOCK_NOTE_BLOCK_PLING");
        adSoundVolume = (float) config.getDouble(PATH_AD_SOUND_VOLUME, 1.0);
        adSoundPitch = (float) config.getDouble(PATH_AD_SOUND_PITCH, 1.0);
        adUsage = HexColorUtil.color(config.getString(PATH_AD_MSG_USAGE, "&eИспользуйте /ad [текст]"));
        adNoMoney = HexColorUtil.color(config.getString(PATH_AD_MSG_INSUFFICIENT, "&cНедостаточно средств!"));
        adMoneySuccess = HexColorUtil.color(config.getString(PATH_AD_MSG_SUCCESS, "&aСписано {count} монет!"));

        buyPermission = config.getString(PATH_BUY_PERMISSION, "ney_broadcast.buy");
        buySize = config.getInt(PATH_BUY_MAX_LENGTH, 90);
        buyCost = config.getDouble(PATH_BUY_COST, 2000.0);
        buyTemplate = getStringListWithColor(PATH_BUY_TEMPLATE);
        buySoundEnabled = config.getBoolean(PATH_BUY_SOUND_ENABLED, false);
        buySoundType = config.getString(PATH_BUY_SOUND_TYPE, "BLOCK_NOTE_BLOCK_PLING");
        buySoundVolume = (float) config.getDouble(PATH_BUY_SOUND_VOLUME, 1.0);
        buySoundPitch = (float) config.getDouble(PATH_BUY_SOUND_PITCH, 1.0);
        buyUsage = HexColorUtil.color(config.getString(PATH_BUY_MSG_USAGE, "&eИспользуйте /buy [текст]"));
        buyNoMoney = HexColorUtil.color(config.getString(PATH_BUY_MSG_INSUFFICIENT, "&cНедостаточно средств!"));
        buyMoneySuccess = HexColorUtil.color(config.getString(PATH_BUY_MSG_SUCCESS, "&aСписано {count} монет!"));

    }

    private List<String> getStringListWithColor(String path) {
        return config.getStringList(path).stream()
                .map(HexColorUtil::color)
                .collect(Collectors.toList());
    }

    public boolean isEnabled() { return enabled; }

    public double getCost(@NotNull String command) {
        return switch (command.toLowerCase()) {
            case "bc" -> bcCost;
            case "ad" -> adCost;
            case "buy" -> buyCost;
            default -> 0.0;
        };
    }

    public int getMaxSize(@NotNull String command) {
        return switch (command.toLowerCase()) {
            case "bc" -> bcSize;
            case "ad" -> adSize;
            case "buy" -> buySize;
            default -> 90;
        };
    }

    public List<String> getTemplate(@NotNull String command) {
        return switch (command.toLowerCase()) {
            case "bc" -> bcTemplate;
            case "ad" -> adTemplate;
            case "buy" -> buyTemplate;
            default -> List.of();
        };
    }

    public boolean isSoundEnabled(@NotNull String command) {
        return switch (command.toLowerCase()) {
            case "bc" -> bcSoundEnabled;
            case "ad" -> adSoundEnabled;
            case "buy" -> buySoundEnabled;
            default -> false;
        };
    }

    public String getSoundType(@NotNull String command) {
        return switch (command.toLowerCase()) {
            case "bc" -> bcSoundType;
            case "ad" -> adSoundType;
            case "buy" -> buySoundType;
            default -> "BLOCK_NOTE_BLOCK_PLING";
        };
    }

    public float getSoundVolume(@NotNull String command) {
        return switch (command.toLowerCase()) {
            case "bc" -> bcSoundVolume;
            case "ad" -> adSoundVolume;
            case "buy" -> buySoundVolume;
            default -> 1.0f;
        };
    }

    public float getSoundPitch(@NotNull String command) {
        return switch (command.toLowerCase()) {
            case "bc" -> bcSoundPitch;
            case "ad" -> adSoundPitch;
            case "buy" -> buySoundPitch;
            default -> 1.0f;
        };
    }

    public String getPermission(@NotNull String command) {
        return switch (command.toLowerCase()) {
            case "bc" -> bcPermission;
            case "ad" -> adPermission;
            case "buy" -> buyPermission;
            default -> "ney_broadcast." + command.toLowerCase();
        };
    }

    public String getUsageKey(@NotNull String command) {
        return switch (command.toLowerCase()) {
            case "bc" -> bcUsage;
            case "ad" -> adUsage;
            case "buy" -> buyUsage;
            default -> "&cИспользуйте команду правильно.";
        };
    }

    public String getAndTextKey(@NotNull String command) {
        return msgColorCode;
    }

    public String getNoMoneyKey(@NotNull String command) {
        return switch (command.toLowerCase()) {
            case "bc" -> bcNoMoney;
            case "ad" -> adNoMoney;
            case "buy" -> buyNoMoney;
            default -> "&cНедостаточно средств.";
        };
    }

    public String getMoneySuccessKey(@NotNull String command) {
        return switch (command.toLowerCase()) {
            case "bc" -> bcMoneySuccess;
            case "ad" -> adMoneySuccess;
            case "buy" -> buyMoneySuccess;
            default -> "&aОплата прошла успешно.";
        };
    }

    public String getNoPermissionKey() {
        return msgNoPermission;
    }

    public String getNoConsoleKey() {
        return msgNoConsole;
    }

    public void reload() {
        loadConfig();
        cacheConfigValues();
    }
}