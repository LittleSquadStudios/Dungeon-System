package com.littlesquad.dungeon.placeholder;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.internal.SessionManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.OfflinePlayer;

import java.time.Instant;

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
            case "name" -> {
                final DungeonSession session;
                yield (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? session
                        .getDungeon()
                        .displayName()
                        : "§cNot in dungeon";
            }
            case "pvp" -> {
                final DungeonSession session;
                yield (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? session
                        .getDungeon()
                        .status()
                        .isPvp()
                        ? "§aEnabled"
                        : "§cDisabile"
                        : "§cNot in dungeon";
            }
            case "globalkills" -> {
                final DungeonSession session;
                yield (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? String.valueOf(session
                        .getDungeon()
                        .status()
                        .totalKills())
                        : "§cNot in dungeon";
            }
            case "partykills" -> {
                final AbstractParty party;
                final DungeonSession session;
                yield String.valueOf((party = Main
                        .getMMOCoreAPI()
                        .getPlayerData(player)
                        .getParty()) != null
                        ? (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? session
                        .getDungeon()
                        .status()
                        .partyKills(party)
                        : "§cNot in dungeon"
                        : (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? session
                        .getDungeon()
                        .status()
                        .playerKills(player.getUniqueId())
                        : "§cNot in dungeon");
            }
            case "personalkills" -> {
                final DungeonSession session;
                yield (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? String.valueOf(session
                        .getDungeon()
                        .status()
                        .playerKills(player.getUniqueId()))
                        : "§cNot in dungeon";
            }
            case "sessionkills" -> {
                final DungeonSession session;
                yield (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? String.valueOf(session
                        .kills())
                        : "§cNot in dungeon";
            }
            case "globaldeaths" -> {
                final DungeonSession session;
                yield (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? String.valueOf(session
                        .getDungeon()
                        .status()
                        .totalDeaths())
                        : "§cNot in dungeon";
            }
            case "partydeaths" -> {
                final AbstractParty party;
                final DungeonSession session;
                yield String.valueOf((party = Main
                        .getMMOCoreAPI()
                        .getPlayerData(player)
                        .getParty()) != null
                        ? (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? session
                        .getDungeon()
                        .status()
                        .partyDeaths(party)
                        : "§cNot in dungeon"
                        : (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? session
                        .getDungeon()
                        .status()
                        .playerDeaths(player.getUniqueId())
                        : "§cNot in dungeon");
            }
            case "personaldeaths" -> {
                final DungeonSession session;
                yield (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? String.valueOf(session
                        .getDungeon()
                        .status()
                        .playerDeaths(player.getUniqueId()))
                        : "§cNot in dungeon";
            }
            case "sessiondeaths" -> {
                final DungeonSession session;
                yield (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? String.valueOf(session
                        .deaths())
                        : "§cNot in dungeon";
            }
            case "players" -> {
                final DungeonSession session;
                yield (session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null
                        ? String.valueOf(session
                        .getDungeon()
                        .status()
                        .currentPlayers())
                        : "§cNot in dungeon";
            }
            case "timein" -> {
                final DungeonSession session;
                if ((session = SessionManager
                        .getInstance()
                        .getSession(player.getUniqueId()))
                        != null) {
                    yield String.valueOf(Instant
                            .ofEpochMilli(
                                    Instant.now().toEpochMilli()
                                    - session.enterTime().toEpochMilli())
                            .getEpochSecond());
                } yield "§cNot in dungeon";
            }
            default -> null;
        };
    }
}
