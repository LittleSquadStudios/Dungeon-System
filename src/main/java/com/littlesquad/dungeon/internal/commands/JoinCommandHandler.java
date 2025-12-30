package com.littlesquad.dungeon.internal.commands;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.internal.DungeonManager;
import com.littlesquad.dungeon.internal.SessionManager;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;

public final class JoinCommandHandler {

    static boolean onLeave(final CommandSender sender) {
        if (!(sender instanceof Player p))
            return false;

        final DungeonSession session = SessionManager
                .getInstance()
                .getSession(p.getUniqueId());

        if (session != null) {
            session.getDungeon()
                    .onExit(p);
            SessionManager
                    .getInstance()
                    .endSession(p.getUniqueId(),
                            ExitReason.QUIT);
        }

        return true;
    }

    static boolean onJoin ( final CommandSender sender, final SecureRandom random, final @NotNull String...args){

        if (!(sender instanceof Player p))
            return false;


        final String dungeonName = args[1];

        final Dungeon dungeonToJoin = DungeonManager
                .getDungeonManager()
                .getDungeon(dungeonName)
                .orElse(DungeonManager
                        .getDungeonManager()
                        .getAllDungeons()
                        .get(random.nextInt(0,
                                DungeonManager
                                        .getDungeonManager()
                                        .getDungeonCount())
                        ));

        if (!dungeonName.equals(dungeonToJoin.id())) { // If dungeon is another so the one chosen by player does not exist then handle this

            p.sendMessage("This dungeon is not available, however you can try this out -> " +
                    dungeonToJoin.displayName() +
                    "with this id: " +
                    dungeonToJoin.id());

        } else switch (dungeonToJoin.tryEnter(p)) { // Else handle the response from tryEnter
            case FAILURE_PER_LEVEL -> {
                p.sendMessage("Your level or your party level isn't enough");

                dungeonToJoin.getEntrance().levelFallbackCommands().forEach(cmd ->
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                PlaceholderFormatter.formatPerPlayer(cmd, p))));
            }
            case FAILURE_PER_DUNGEON_BLOCKED -> p.sendMessage("Dungeon is blocked");
            case FAILURE_PER_SLOTS -> {
                p.sendMessage("There's no space left");

                dungeonToJoin.getEntrance().maxSlotsFallbackCommands().forEach(cmd ->
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                PlaceholderFormatter.formatPerPlayer(cmd, p))));
            }
            case FAILURE_PER_ALREADY_PROCESSING ->
                    p.sendMessage("One of your teammate has already started dungeon join process");
            case FAILURE_PER_PARTY -> {
                p.sendMessage("Your party sucks");

                dungeonToJoin.getEntrance().partyFallbackCommands().forEach(cmd ->
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                PlaceholderFormatter.formatPerPlayer(cmd, p))));
            }
            case FAILURE_PER_SENDER_ALREADY_IN -> p.sendMessage("You're already in a dungeon");
            case FAILURE_PER_MEMBER_ALREADY_IN -> p.sendMessage("There's already a party member in");
            case SUCCESS_PARTY -> {

                final AbstractParty playerParty = Main.getMMOCoreAPI()
                        .getPlayerData(p)
                        .getParty();

                if (playerParty == null) {
                    p.sendMessage("You should be in a party in order to join this dungeon!");
                    return false;
                }

                final Player[] partyMembers = playerParty.getOnlineMembers()
                        .stream()
                        .map(PlayerData::getPlayer)
                        .toArray(Player[]::new);

                if (partyMembers.length == 0) {
                    p.sendMessage("No party member is online!");
                    return false;
                }

                dungeonToJoin.onEnter(partyMembers);

            }
            case SUCCESS_SOLO -> dungeonToJoin.onEnter(p);
        }
        return true;
    }

}