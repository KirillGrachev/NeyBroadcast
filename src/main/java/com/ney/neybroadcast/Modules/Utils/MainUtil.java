package com.ney.neybroadcast.Modules.Utils;

import com.ney.neybroadcast.NeyBroadcast;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

import static com.ney.neybroadcast.Modules.Broadcast.Commands.moneycountfinal;
import static com.ney.neybroadcast.Modules.Utils.Hex.color;

public class MainUtil {

    public boolean Enabled = getBoolean("Settings.enabled");
    public boolean bcEnabled = getBoolean("Messages.bc.enabled");
    public List<String> bcTemplate = getStringList("Messages.bc.text");
    public List<String> adTemplate = getStringList("Messages.ad.text");
    public boolean adEnabled = getBoolean("Messages.ad.enabled");
    public List<String> buyTemplate = getStringList("Messages.buy.text");
    public boolean buyEnabled = getBoolean("Messages.buy.enabled");

    public double getDouble(String message) {
        return NeyBroadcast.getInstance().getConfig().getDouble(message);
    }

    public Sound getSound(String message) {

        String soundName = NeyBroadcast.getInstance().getConfig().getString(message);
        if (soundName != null) try { return Sound.valueOf(soundName); } catch (IllegalArgumentException e) {e.printStackTrace(); }

        return null;

    }

    public int getInteger (String message) {
        return NeyBroadcast.getInstance().getConfig().getInt(message);
    }

    public String getString (String message) {
        return color(NeyBroadcast.getInstance().getConfig().getString(message));
    }

    public boolean getBoolean(String message) {
        return NeyBroadcast.getInstance().getConfig().getBoolean(message);
    }

    public List<String> getStringList(String message) {

        return NeyBroadcast.getInstance().getConfig().getStringList(message)
                .stream()
                .map(Hex::color)
                .collect(Collectors.toList());

    }

    public void sendMessage(String messageText, Player player, Sound soundType, float volume, float pitch, boolean soundEnabled) {

        getStringList(messageText).stream()

                .map(message ->
                        message.replaceAll("\\{player_name\\}", player.getName())
                                .replaceAll("\\{count\\}", String.valueOf(moneycountfinal))
                ).forEach(player::sendMessage);

        if (soundEnabled) player.playSound(player.getLocation(), soundType, volume, pitch);

    }
}