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

public final class DungeonCommand implements CommandExecutor, TabCompleter {

    private final SecureRandom random = new SecureRandom();
    private final DungeonManager dungeonManager;

    // TODO: migrare join features in un JoinCommandHandler

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
