package me.redned.signmanager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlaceholderParser {

    public static String replacePlaceholders(Player player, String text) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }
}
