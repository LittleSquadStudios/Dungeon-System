package com.littlesquad.dungeon.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public final class PlaceholderFormatter {

    private PlaceholderFormatter () {}

    public static String formatPerPlayer (final String s, final Player p) {
        p.sendMessage("test 3");
        return PlaceholderAPI.setPlaceholders(p, s);
    }
}
