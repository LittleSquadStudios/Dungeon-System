package com.littlesquad.dungeon.internal;

import com.littlesquad.dungeon.api.AbstractDungeon;
import com.littlesquad.dungeon.api.TypeFlag;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.entrance.Entrance;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.event.Event;
import com.littlesquad.dungeon.api.status.Status;
import com.littlesquad.dungeon.internal.file.DungeonParser;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public final class DungeonImpl extends AbstractDungeon {

    public DungeonImpl(final DungeonParser parser) {
        super(parser);
    }

    @Override
    public String id() {
        return "";
    }

    @Override
    public String displayName() {
        return "TestDungeon";
    }

    @Override
    public Set<TypeFlag> typeFlags() {
        final Set<TypeFlag> flags = new HashSet<>(3);

        if (getParser().isTimeLimited()) {
            flags.add(TypeFlag.TIMED);
        }

        if (getParser().isPvP()) {
            flags.add(TypeFlag.PVP_ENABLED);
        }

        // if (getParser().getBossRoom()) TODO: Not implemented yet, muvt draky

        return flags;
    }

    @Override
    public World getWorld() {
        return Bukkit.getWorld("world");
    }

    @Override
    public Entrance getEntrance() {
        return getParser().getEntrance();
    }

    @Override
    public Event[] getEvents() {
        return new Event[0];
    }

    @Override
    public Checkpoint[] getCheckpoints() {
        return new Checkpoint[0];
    }

    @Override
    public BossRoom[] getBossRooms() {
        return new BossRoom[0];
    }

    @Override
    public ExitReason forceExit(Player player) {
        return null;
    }

    @Override
    public Status status() {
        return null;
    }

    @Override
    public Checkpoint getCheckPoint(String checkPointId) {
        return null;
    }
}
