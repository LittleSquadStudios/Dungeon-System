package com.littlesquad.dungeon.placeholder;

import com.littlesquad.Main;
import com.littlesquad.dungeon.internal.SessionManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.Indyuce.mmocore.party.AbstractParty;
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
            case "name" -> SessionManager
                    .getInstance()
                    .getSession(player.getUniqueId())
                    .getDungeon()
                    .displayName();
            case "pvp" -> SessionManager
                    .getInstance()
                    .getSession(player.getUniqueId())
                    .getDungeon()
                    .status()
                    .isPvp()
                    ? "§aEnabled"
                    : "§cDisabile";
            case "globalkills" -> String.valueOf(SessionManager
                    .getInstance()
                    .getSession(player.getUniqueId())
                    .getDungeon()
                    .status()
                    .totalKills());
            case "partykills" -> {
                final AbstractParty party;
                yield String.valueOf((party = Main
                        .getMMOCoreAPI()
                        .getPlayerData(player)
                        .getParty()) != null
                        ? SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId())
                        .getDungeon()
                        .status()
                        .partyKills(party)
                        : SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId())
                        .getDungeon()
                        .status()
                        .playerKills(player.getUniqueId()));
            }
            case "personalkills" -> String.valueOf(SessionManager
                    .getInstance()
                    .getSession(player.getUniqueId())
                    .getDungeon()
                    .status()
                    .playerKills(player.getUniqueId()));
            case "sessionkills" -> String.valueOf(SessionManager
                    .getInstance()
                    .getSession(player.getUniqueId())
                    .kills());
            case "globaldeaths" -> String.valueOf(SessionManager
                    .getInstance()
                    .getSession(player.getUniqueId())
                    .getDungeon()
                    .status()
                    .totalDeaths());
            case "partydeaths" -> {
                final AbstractParty party;
                yield String.valueOf((party = Main
                        .getMMOCoreAPI()
                        .getPlayerData(player)
                        .getParty()) != null
                        ? SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId())
                        .getDungeon()
                        .status()
                        .partyDeaths(party)
                        : SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId())
                        .getDungeon()
                        .status()
                        .playerDeaths(player.getUniqueId()));
            }
            case "personaldeaths" -> String.valueOf(SessionManager
                    .getInstance()
                    .getSession(player.getUniqueId())
                    .getDungeon()
                    .status()
                    .playerDeaths(player.getUniqueId()));
            case "sessiondeaths" -> String.valueOf(SessionManager
                    .getInstance()
                    .getSession(player.getUniqueId())
                    .deaths());
            case "players" -> String.valueOf(SessionManager
                    .getInstance()
                    .getSession(player.getUniqueId())
                    .getDungeon()
                    .status()
                    .currentPlayers());
            default -> null;
        };
    }
}
