package com.ney.broadcast.tab;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {

        String commandName = command.getName().toLowerCase();
        if (!Arrays.asList("bc", "ad", "buy").contains(commandName)) {
            return null;
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {

            completions.add("Привет всем!");
            completions.add("Заходите на сервер!");
            completions.add("Скидки до 50% сегодня!");

            String current = args[0].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(current));

        }

        return completions.isEmpty() ? null : completions;

    }
}