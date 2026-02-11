package com.littlesquad.dungeon.internal.event;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.event.TimedEvent;
import com.littlesquad.dungeon.internal.utils.CommandUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class TimedEventImpl extends TimedEvent {
    private final Dungeon dungeon;
    private final String id;
    private final List<String> commands;

    private final boolean isFixed;
    private final long timeAmount;
    private final TimeUnit timeUnit;

    private final Map<Player, ScheduledFuture<?>> players;

    public TimedEventImpl (final Dungeon dungeon,
                           final String id,
                           final List<String> commands,
                           final boolean isFixed,
                           final long timeAmount,
                           final TimeUnit timeUnit) {
        this.dungeon = dungeon;
        this.id = id;
        this.commands = commands;
        this.isFixed = isFixed;
        this.timeAmount = timeAmount;
        this.timeUnit = timeUnit;
        players = new ConcurrentHashMap<>();
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onQuit (final PlayerQuitEvent e) {
        deActiveFor(e.getPlayer());
    }

    public boolean isFixed () {
        return isFixed;
    }

    public long timeAmount () {
        return timeAmount;
    }
    public TimeUnit timeUnit () {
        return timeUnit;
    }

    public Dungeon getDungeon () {
        return dungeon;
    }
    public String getID () {
        return id;
    }

    public List<String> commands () {
        return commands;
    }

    public void triggerActivation (final Player... players) {
        Arrays.stream(players)
                .parallel()
                .forEach(player -> {
                    this.players.computeIfAbsent(player, _ -> Main.getScheduledExecutor().schedule(() -> {
                        if (this.players.remove(player) != null)
                            CommandUtils.executeMulti(
                                    Bukkit.getConsoleSender(),
                                    commands,
                                    player);
                            },
                            isFixed ? timeAmount : new Random(System.nanoTime()).nextLong(timeUnit.toMillis(timeAmount)),
                            isFixed ? timeUnit : TimeUnit.MILLISECONDS));
                    if (!player.isOnline())
                        this.players
                                .remove(player)
                                .cancel(false);
                });
    }
    public boolean isActiveFor (final Player... players) {
        return Arrays.stream(players)
                .parallel()
                .allMatch(this.players::containsKey);
    }

    public void deActiveFor (final Player... players) {
        Arrays.stream(players)
                .parallel()
                .forEach(player -> {
                    final ScheduledFuture<?> task;
                    if ((task = this.players.remove(player)) != null)
                        task.cancel(false);
                });
    }

    public void close () {
        PlayerQuitEvent.getHandlerList().unregister(this);
        deActiveFor(players.keySet().toArray(new Player[0]));
    }
}
