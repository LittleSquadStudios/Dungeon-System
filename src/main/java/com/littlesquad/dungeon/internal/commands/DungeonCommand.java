package com.littlesquad.dungeon.internal.commands;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.internal.DungeonManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public final class DungeonCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if(!command.getName().equals("dungeon"))
            return false;

        if (!(sender instanceof Player p))
            return false;

        switch (args.length) {
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
                                        .get(new Random(System.currentTimeMillis())
                                                .nextInt(0,
                                                        DungeonManager
                                                        .getDungeonManager()
                                                        .getDungeonCount()) // TODO: To create a standard random
                                        ));

                        if (!dungeonName.equals(dungeonToJoin.id())) {

                            p.sendMessage("This dungeon is not available, however you can try this out -> " +
                                    dungeonToJoin.displayName() +
                                    "with this id: " +
                                    dungeonToJoin.id());

                        }

                    }
                }
            }
            default -> System.out.println("Something");
        }

        return false;
    }

}
