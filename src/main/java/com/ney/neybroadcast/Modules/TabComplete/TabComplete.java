package com.ney.neybroadcast.Modules.TabComplete;

import com.ney.neybroadcast.Modules.Utils.MainUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TabComplete implements TabCompleter {

    private final MainUtil mainUtil = new MainUtil();

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        List<String> completions = new ArrayList<>();

        if (!mainUtil.Enabled) return completions;
        if (command.equals("bc") || command.equals("ad") || command.equals("buy")) return completions;

        return completions;
    }
}