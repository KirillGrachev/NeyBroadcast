package com.ney.broadcast.service;

import com.ney.broadcast.util.HexColorUtil;
import com.ney.broadcast.config.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.ney.broadcast.util.HexColorUtil.color;


public class BroadcastService {

    private final ConfigManager config;
    private final Economy economy;

    public BroadcastService(@NotNull ConfigManager config,
                            @NotNull Economy economy) {
        this.config = config;
        this.economy = economy;
    }

    public void handleCommand(@NotNull CommandSender sender,
                              @NotNull String command,
                              @NotNull String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(HexColorUtil.color(config.getNoConsoleKey()));
            return;

        }

        if (args.length == 0) {

            sender.sendMessage(color(config.getUsageKey(command)));
            return;

        }

        String message = String.join(" ", args);

        if (!player.hasPermission(config.getPermission(command))) {

            player.sendMessage(color(config.getNoPermissionKey()));
            return;

        }

        if (message.contains("&") || message.contains("§")) {

            player.sendMessage(color(config.getColorCodeTextKey(command)));
            return;

        }

        int maxSize = config.getMaxSize(command);
        if (message.length() > maxSize) {

            String tooLargeMsg = color(config.getTooLargeKey().replace(
               "{length}", String.valueOf(config.getMaxSize(command))
            ));

            player.sendMessage(tooLargeMsg);
            return;

        }

        double cost = config.getCost(command);
        if (cost > 0 && economy.getBalance(player) < cost) {

            player.sendMessage(color(config.getNoMoneyKey(command)));
            return;

        }

        if (cost > 0) {

            economy.withdrawPlayer(player, cost);

            String successMsg = color(config.getMoneySuccessKey(command))
                    .replace("{player_name}", player.getName())
                    .replace("{count}", String.valueOf((int) cost));

            player.sendMessage(successMsg);

        }

        List<String> template = config.getTemplate(command);
        for (String line : template) {

            String formatted = color(line
                    .replace("%player%", player.getName())
                    .replace("%message%", message));

            Bukkit.getOnlinePlayers().forEach(p ->
                    p.sendMessage(formatted));

        }

        if (config.isSoundEnabled(command)) {

            Sound sound = Sound.valueOf(config.getSoundType(command));

            float volume = config.getSoundVolume(command);
            float pitch = config.getSoundPitch(command);

            player.playSound(player.getLocation(), sound, volume, pitch);

        }
    }
}