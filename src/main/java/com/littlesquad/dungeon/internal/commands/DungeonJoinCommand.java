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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class DungeonJoinCommand implements CommandExecutor, TabCompleter {

    private final SecureRandom random = new SecureRandom();
    private final DungeonManager dungeonManager;

    public DungeonJoinCommand(DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
    }


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
            case 1 -> {
                final String argument = args[0];
                switch (argument) {
                    case "leave" -> {
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
                        } else p.sendMessage("You're not in a dungeon, what the fuck are you trying to do");
                    }
                    default -> p.sendMessage("Not a valid command");
                }
            }
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
                                p.sendMessage("Your level or your party level isn't enough");

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
                            case SUCCESS_SOLO -> {
                                p.sendMessage("test");
                                dungeonToJoin.onEnter(p);
                            }
                        }

                    }
                }
            }
            default -> System.out.println("Something");
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("join");
            completions.add("leave");
            completions.add("list");
            completions.add("info");
            completions.add("trigger");

            return completions.stream()
                    .filter(completion -> completion.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "join":
                case "info":
                    return dungeonManager.getAllDungeons().stream()
                            .map(Dungeon::id)
                            .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());

                case "trigger":
                    completions.add("first_event");
                    completions.add("objective_event");
                    completions.add("structural_event_A");
                    return completions.stream()
                            .filter(completion -> completion.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("trigger")) {
            return null;
        }

        return completions;
    }
}
