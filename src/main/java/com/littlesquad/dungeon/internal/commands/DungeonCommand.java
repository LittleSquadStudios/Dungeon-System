package com.littlesquad.dungeon.internal.commands;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.internal.DungeonManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.ArrayList;
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

        if (args.length == 1) {
            completions.add("join");
            completions.add("leave");
            completions.add("list");
            completions.add("info");
            completions.add("event");

            return completions.stream()
                    .filter(completion -> completion.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2)
            if (args[1].equals("info") || args[1].equals("join"))
                return dungeonManager.getAllDungeons().stream()
                        .map(Dungeon::id)
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            else if (args[1].equals("event"))
                return List.of("trigger");

        if (args.length == 3 && args[0].equalsIgnoreCase("event")) {
            final String dungeon = args[1];

            return null;
        }

        return completions;
    }
}
