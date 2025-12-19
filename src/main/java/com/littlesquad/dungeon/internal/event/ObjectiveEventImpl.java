package com.littlesquad.dungeon.internal.event;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.event.ObjectiveEvent;
import com.littlesquad.dungeon.internal.file.RequirementsParser;
import org.bukkit.entity.Player;

import java.util.List;

public final class ObjectiveEventImpl extends ObjectiveEvent {
    private final Dungeon dungeon;
    private final String id;
    private final List<String> commands;

    private final String checkpoint;
    private Checkpoint rCheckpoint;
    private final String boosRoom;
    private BossRoom rBossRoom;

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
        super(parser);
    }

    public Checkpoint checkpointToUnlock () {
        return rCheckpoint != null ? rCheckpoint : (rCheckpoint = null); //TODO: get checkpoint from string!
    }
    public BossRoom bossRoomToUnlock () {
        return rBossRoom != null ? rBossRoom : (rBossRoom = null); //TODO: get boss room from string!
    }

    @Override
    public void executeCommandsFor (final Player... players) {

    }
    @Override
    public void deActiveFor (final Player... players) {

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

    @Override
    public void triggerActivation(Player... players) {

    }

    @Override
    public boolean isActiveFor(Player... players) {
        return false;
    }
}
