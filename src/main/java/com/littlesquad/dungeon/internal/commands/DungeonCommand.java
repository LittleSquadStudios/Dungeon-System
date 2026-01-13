package com.littlesquad.dungeon.internal.commands;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.event.Event;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.internal.DungeonManager;
import com.littlesquad.dungeon.internal.SessionManager;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class DungeonCommand implements CommandExecutor, TabCompleter {

    private final SecureRandom random = new SecureRandom();
    private final DungeonManager dungeonManager;

    public DungeonCommand(DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
    }


    @Override
    public boolean onCommand(@NotNull final CommandSender sender,
                             @NotNull final Command command,
                             @NotNull final String label,
                             @NotNull final String @NotNull [] args) {

        if(!command.getName().equals("dungeon"))
            return false;

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "trigger":
                    return EventCommandHandler.onTrigger(sender, args);
                case "deactivate":
                    return EventCommandHandler.onDeactivate(sender, args);
                case "join":
                    return JoinCommandHandler.onJoin(sender, random, args);
                case "leave":
                    return JoinCommandHandler.onLeave(sender);
                case "reload":
                    return ReloadCommandHandler.onReload(sender, args);
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        final List<String> completions = new ArrayList<>();

        if (commandSender instanceof Player p)
            switch (args.length) {
                case 1 -> {
                    completions.add("join");
                    completions.add("leave");
                    completions.add("reload");
                    completions.add("deactivate");
                    completions.add("trigger");

                    return completions.stream()
                            .filter(completion -> completion.toLowerCase().startsWith(args[0].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case 2 -> {
                    switch (args[0]) {
                        case "join" -> {
                            return dungeonManager.getAllDungeons().stream()
                                    .map(Dungeon::id)
                                    .collect(Collectors.toList());
                        }
                        case "trigger", "deactivate" -> {

                            final DungeonSession session;

                            if((session = SessionManager
                                    .getInstance()
                                    .getSession(p.getUniqueId()))
                                    != null) {
                                Arrays.stream(session
                                                .getDungeon()
                                                .getEvents())
                                        .forEach(e ->
                                                completions.add(e.getID()));
                            } else return Collections.singletonList("not-in-dungeon");
                        }
                    }
                }
                case 3 -> {
                    if (args[0].equals("trigger")
                            && SessionManager
                            .getInstance()
                            .getSession(p.getUniqueId())
                            != null)
                        return Bukkit.getOnlinePlayers()
                                .parallelStream()
                                .map(Player::getName)
                                .toList();
                }
            }

        return completions;
    }
}
