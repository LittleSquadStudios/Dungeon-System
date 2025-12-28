package com.littlesquad.dungeon.internal.commands;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.internal.DungeonManager;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import net.Indyuce.mmocore.party.provided.Party;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DungeonJoinCommand implements CommandExecutor {

    private final SecureRandom random = new SecureRandom();

    @Override
    public boolean onCommand(@NotNull final CommandSender sender,
                             @NotNull final Command command,
                             @NotNull final String label,
                             @NotNull final String @NotNull [] args) {

        if(!command.getName().equals("dungeon"))
            return false;

        if (!(sender instanceof Player p))
            return false;

        switch (args.length) {
            case 0 -> p.sendMessage("Choose between subcommands");
            case 1 -> p.sendMessage("Not enough commands");
            case 2 -> {
                final String argument = args[0];
                switch (argument) {
                    case "join" -> {
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
                                p.sendMessage("You or your party don't have enough levels");

                                dungeonToJoin.getEntrance().levelFallbackCommands().forEach(cmd ->
                                        Bukkit.dispatchCommand(
                                                Bukkit.getConsoleSender(),
                                                PlaceholderFormatter.formatPerPlayer(cmd, p)));
                            }
                            case FAILURE_PER_DUNGEON_BLOCKED -> p.sendMessage("Dungeon is blocked");
                            case FAILURE_PER_SLOTS -> {
                                p.sendMessage("There's no space left");

                                dungeonToJoin.getEntrance().maxSlotsFallbackCommands().forEach(cmd ->
                                        Bukkit.dispatchCommand(
                                                Bukkit.getConsoleSender(),
                                                PlaceholderFormatter.formatPerPlayer(cmd, p)));
                            }
                            case FAILURE_PER_ALREADY_PROCESSING -> p.sendMessage("One of your teammate has already started dungeon join process");
                            case FAILURE_PER_PARTY -> {
                                p.sendMessage("Your party sucks");

                                dungeonToJoin.getEntrance().partyFallbackCommands().forEach(cmd ->
                                        Bukkit.dispatchCommand(
                                                Bukkit.getConsoleSender(),
                                                PlaceholderFormatter.formatPerPlayer(cmd, p)));
                            }
                            case SUCCESS -> {

                                final AbstractParty playerParty = Main.getMMOCoreAPI()
                                        .getPlayerData(p)
                                        .getParty();

                                if (playerParty == null) {
                                    p.sendMessage("You should be in a dungeon in order to join this dungeon!");
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
                        }

                    }
                }
            }
            default -> System.out.println("Something");
        }

        return false;
    }

}
