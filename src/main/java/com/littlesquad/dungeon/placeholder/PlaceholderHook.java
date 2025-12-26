package com.littlesquad.dungeon.placeholder;

import com.littlesquad.dungeon.internal.DungeonManager;
import com.littlesquad.dungeon.internal.SessionManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

@SuppressWarnings("NullableProblems")
public final class PlaceholderHook extends PlaceholderExpansion {
    public PlaceholderHook() {}

    @Override
    public String getIdentifier () {
        return "azuredungeon";
    }

    @Override
    public String getAuthor () {
        return "LittleSquad";
    }

    @Override
    public String getVersion () {
        return "1.0.0";
    }

    @Override
    public String onRequest (OfflinePlayer player, String params) {
        if (player == null)
            return null;
        return switch (params.toLowerCase()) {
            case "dungeon" -> SessionManager
                    .getInstance()
                    .getSession(player.getUniqueId())
                    .getDungeon()
                    .displayName();
            default -> null;
        };
    }
}
