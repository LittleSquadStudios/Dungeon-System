package com.littlesquad.dungeon.internal;

import com.littlesquad.dungeon.api.AbstractDungeon;
import com.littlesquad.dungeon.api.TypeFlag;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.entrance.Entrance;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.event.Event;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;

public final class DungeonImpl extends AbstractDungeon {

    public DungeonImpl() {
        super("TestDungeon");
    }

    @Override
    public String displayName() {
        return "TestDungeon";
    }

    @Override
    public Set<TypeFlag> typeFlags() {
        return Set.of(TypeFlag.TIMED,
                TypeFlag.HAS_BOSSROOM,
                TypeFlag.PVP_ENABLED);
    }

    @Override
    public World getWorld() {
        return Bukkit.getWorld("world");
    }

    @Override
    public Entrance getEntrance() {
        return null;
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
    public Checkpoint getCheckPoint(String checkPointId) {
        return null;
    }
}
