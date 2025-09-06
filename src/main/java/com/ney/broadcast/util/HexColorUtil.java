package com.ney.broadcast.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexColorUtil {

    public static @NotNull String color(String text) {

        if (text == null || text.isEmpty()) {
            return "";
        }

        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(text);

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {

            String hexCode = matcher.group();

            if (hexCode.length() != 7 || !isValidHexCode(hexCode)) {
                continue;
            }

            result.append(text, lastEnd, matcher.start());

            // Преобразуем #RRGGBB -> &x&R&R&G&G&B&B
            StringBuilder replacement = new StringBuilder("&x");
            for (int i = 1; i < hexCode.length(); i++) {
                replacement.append("&").append(hexCode.charAt(i));
            }

            result.append(replacement);
            lastEnd = matcher.end();

        }

        result.append(text.substring(lastEnd));
        return ChatColor.translateAlternateColorCodes('&',
                result.toString());

    }

    private static boolean isValidHexCode(@NotNull String code) {

        for (int i = 1; i < code.length(); i++) {

            char c = code.charAt(i);
            if (!Character.isDigit(c) && (c < 'a' || c > 'f')
                    && (c < 'A' || c > 'F')) {
                return false;
            }

        }

        return true;

    }
}