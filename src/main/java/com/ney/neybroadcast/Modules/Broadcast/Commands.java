package com.ney.neybroadcast.Modules.Broadcast;

import com.ney.neybroadcast.Modules.Utils.MainUtil;
import com.ney.neybroadcast.NeyBroadcast;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import static com.ney.neybroadcast.Modules.Utils.Hex.color;

public class Commands implements CommandExecutor {

    private final MainUtil mainUtil = new MainUtil();
    public static int moneycountfinal;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (sender instanceof ConsoleCommandSender) {

            sender.sendMessage(mainUtil.getString("Messages.no_console.text"));
            return true;

        }

        switch (command.getName().toLowerCase()) {

            case "bc" -> handleCommand(sender, args, "Messages.bc.money.dont_have.text", "Messages.bc.money.text", "Messages.bc.money.enabled", "Messages.bc.money.count", "Messages.bc.usage.text", "Messages.bc.and.text", "Messages.bc.size", "ney_broadcast.bc", mainUtil.bcTemplate.toArray(new String[0]), "Messages.bc.sound.type", "Messages.bc.sound.volume", "Messages.bc.sound.pitch", mainUtil.bcEnabled);
            case "ad" -> handleCommand(sender, args, "Messages.ad.money.dont_have.text", "Messages.ad.money.text", "Messages.ad.money.enabled", "Messages.ad.money.count", "Messages.ad.usage.text", "Messages.ad.and.text", "Messages.ad.size", "ney_broadcast.ad", mainUtil.adTemplate.toArray(new String[0]), "Messages.ad.sound.type", "Messages.ad.sound.volume", "Messages.ad.sound.pitch", mainUtil.adEnabled);
            case "buy" -> handleCommand(sender, args, "Messages.buy.money.dont_have.text", "Messages.buy.money.text", "Messages.buy.money.enabled", "Messages.buy.money.count", "Messages.buy.usage.text", "Messages.buy.and.text", "Messages.buy.size", "ney_broadcast.buy", mainUtil.buyTemplate.toArray(new String[0]), "Messages.buy.sound.type", "Messages.buy.sound.volume", "Messages.buy.sound.pitch", mainUtil.buyEnabled);

        }

        return true;
    }

    private void handleCommand(CommandSender sender, String[] args, String moneyText2, String moneyText, String money, String moneycount, String usage, String messageConfig, String size, String perm, String[] template, String soundtype, String volume1, String pitch1, boolean enabled) {

        Player player = (Player) sender;
        String message = String.join(" ", args);

        Sound soundType = mainUtil.getSound(soundtype);
        float volume = (float) mainUtil.getDouble(volume1);
        float pitch = (float) mainUtil.getDouble(pitch1);

        if (!player.hasPermission(perm)) {

            player.sendMessage(mainUtil.getString("Messages.no_permission.text"));
            return;

        } else if (args.length == 0) {

            mainUtil.sendMessage(usage, player, soundType, volume, pitch, enabled);
            return;

        } else if (message.contains("&") || message.contains("§")) {

            mainUtil.sendMessage(messageConfig, player, soundType, volume, pitch, enabled);
            return;

        } else if (mainUtil.getBoolean(money)) {

           moneycountfinal = Integer.parseInt(mainUtil.getString(moneycount));

           if (NeyBroadcast.getInstance().getEconomy().getBalance(player) < moneycountfinal) {

               mainUtil.sendMessage(moneyText2, player, soundType, volume, pitch, enabled);
               return;

           }

           NeyBroadcast.getInstance().getEconomy().withdrawPlayer(player, moneycountfinal);
           Bukkit.getScheduler().runTaskLater(NeyBroadcast.getInstance(), () -> mainUtil.sendMessage(moneyText, player, soundType, volume, pitch, enabled), 10L);

        }

        String[] broadcastMessage = template;

        if (message.length() < mainUtil.getInteger(size)) {

            for (String msg : broadcastMessage) {

                String formattedMsg = color(msg.replace("%player%", player.getName()).replace("%message%", message));
                Bukkit.getOnlinePlayers().forEach(playerforbc -> playerforbc.sendMessage(formattedMsg));

            }
        } else player.sendMessage(mainUtil.getString("Messages.no_permission.text"));
    }
}