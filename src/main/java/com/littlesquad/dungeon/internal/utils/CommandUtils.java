package com.littlesquad.dungeon.internal.utils;

import com.littlesquad.Main;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.stream.Stream;

public final class CommandUtils {
    private CommandUtils () {}

    public static BukkitTask execute (final CommandSender sender,
                                      final String c,
                                      final Player p) {
        return Bukkit.getScheduler().runTask(
                Main.getInstance(),
                () -> Bukkit.dispatchCommand(
                        sender,
                        PlaceholderFormatter.formatPerPlayer(c, p)));
    }

    public static BukkitTask executeMulti (final CommandSender sender,
                                     final List<String> cl,
                                     final Player p) {
        return Bukkit.getScheduler().runTask(
                Main.getInstance(),
                () -> cl.forEach(c -> Bukkit.dispatchCommand(
                        sender,
                        PlaceholderFormatter.formatPerPlayer(c, p))));
    }

    public static BukkitTask executeForMulti (final CommandSender sender,
                                        final String c,
                                        final Player... ps) {
        return Bukkit.getScheduler().runTask(
                Main.getInstance(),
                () -> Stream.of(ps).forEach(p -> Bukkit.dispatchCommand(
                        sender,
                        PlaceholderFormatter.formatPerPlayer(c, p))));
    }

    public static BukkitTask executeMultiForMulti (final CommandSender sender,
                                             final List<String> cl,
                                             final Player... ps) {
        return Bukkit.getScheduler().runTask(
                Main.getInstance(),
                () -> cl.forEach(c -> Stream
                        .of(ps)
                        .forEach(p -> Bukkit.dispatchCommand(
                                sender,
                                PlaceholderFormatter.formatPerPlayer(c, p)))));
    }
}
