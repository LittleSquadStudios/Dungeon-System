package com.littlesquad.dungeon.internal.utils;

import com.littlesquad.Main;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class CommandUtils {
    private CommandUtils () {}

    public static BukkitTask execute (final CommandSender sender,
                                      final String c,
                                      final Player p) {
        final String s = PlaceholderFormatter.formatPerPlayer(c, p);
        return Bukkit.getScheduler().runTask(
                Main.getInstance(),
                () -> Bukkit.dispatchCommand(sender, s));
    }

    public static BukkitTask executeMulti (final CommandSender sender,
                                           final List<String> cl,
                                           final Player p) {
        final int size;
        final String[] clCopy = new String[size = cl.size()];
        IntStream.range(0, size)
                .parallel()
                .forEach(i -> clCopy[i] =
                        PlaceholderFormatter.formatPerPlayer(
                                cl.get(i),
                                p));
        return Bukkit.getScheduler().runTask(
                Main.getInstance(),
                () -> {
                    for (int i = 0; i < size; ++i)
                        Bukkit.dispatchCommand(
                                sender,
                                clCopy[i]);
                });
    }

    public static BukkitTask executeForMulti (final CommandSender sender,
                                              final String c,
                                              final Player... ps) {
        final String[] cl = Stream
                .of(ps)
                .parallel()
                .map(p -> PlaceholderFormatter.formatPerPlayer(c, p))
                .toArray(String[]::new);
        return Bukkit.getScheduler().runTask(
                Main.getInstance(),
                () -> Stream.of(cl)
                        .forEach(cmd -> Bukkit.dispatchCommand(
                                sender,
                                cmd)));
    }

    public static BukkitTask executeMultiForMulti (final CommandSender sender,
                                                   final List<String> cl,
                                                   final Player... ps) {
        final String[] cs = cl
                .parallelStream()
                .flatMap(c -> Stream
                        .of(ps)
                        .parallel()
                        .map(p -> PlaceholderFormatter.formatPerPlayer(c, p)))
                .toArray(String[]::new);
        return Bukkit.getScheduler().runTask(
                Main.getInstance(),
                () -> Stream.of(cs)
                        .forEach(cmd -> Bukkit.dispatchCommand(
                                sender,
                                cmd)));
    }
}
