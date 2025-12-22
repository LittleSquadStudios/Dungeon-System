package com.littlesquad.dungeon.internal.commands;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.internal.DungeonManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DungeonJoinCommand implements CommandExecutor {

    private Random random = new Random();

    public DungeonJoinCommand() {
        final ScheduledExecutorService sex = Executors.newSingleThreadScheduledExecutor();

        sex.scheduleAtFixedRate(() -> random = new Random(
                System.currentTimeMillis()),
                0,
                5,
                TimeUnit.HOURS);
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
