package com.littlesquad.dungeon.internal.event;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.event.ObjectiveEvent;
import com.littlesquad.dungeon.internal.file.RequirementsParser;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ObjectiveEventImpl extends ObjectiveEvent {
    private final Dungeon dungeon;
    private final String id;
    private final List<String> commands;

    //Lazy loading for avoiding initialization order mess!
    private final String checkpoint;
    private Checkpoint rCheckpoint;
    private final String boosRoom;
    private BossRoom rBossRoom;

    private final Set<Player> players;

    public ObjectiveEventImpl (final Dungeon dungeon,
                               final String id,
                               final List<String> commands,
                               final String checkpoint,
                               final String boosRoom,
                               final RequirementsParser parser) {
        this.dungeon = dungeon;
        this.id = id;
        this.commands = commands;
        this.checkpoint = checkpoint;
        this.boosRoom = boosRoom;
        players = ConcurrentHashMap.newKeySet();
        super(parser);
    }

    public Checkpoint checkpointToUnlock () {
        return rCheckpoint != null ? rCheckpoint : (rCheckpoint = null); //TODO: get checkpoint from string!
    }
    public BossRoom bossRoomToUnlock () {
        return rBossRoom != null ? rBossRoom : (rBossRoom = null); //TODO: get boss room from string!
    }

    public void executeCommandsFor (final Player... players) {
        Arrays.stream(players)
                .parallel()
                .filter(this.players::remove)
                .forEach(player -> {
                    for (final String command : commands)
                        Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                PlaceholderFormatter.formatPerPlayer(command, player));
                });
    }
    public void deActiveFor (final Player... players) {
        Arrays.stream(players)
                .parallel()
                .forEach(this.players::remove);
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
                .forEach(this.players::add);
    }
    public boolean isActiveFor (final Player... players) {
        return Arrays.stream(players)
                .parallel()
                .allMatch(this.players::contains);
    }
}
