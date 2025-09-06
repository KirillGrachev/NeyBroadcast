package com.ney.broadcast.command;

import com.ney.broadcast.service.BroadcastService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BroadcastCommand implements CommandExecutor {

    private final BroadcastService broadcastService;

    public BroadcastCommand(@NotNull BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label, String[] args) {

        broadcastService.handleCommand(sender, command.getName(), args);
        return true;

    }
}