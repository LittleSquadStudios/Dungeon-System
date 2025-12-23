package com.littlesquad.dungeon.internal;

import com.littlesquad.dungeon.api.AbstractDungeon;
import com.littlesquad.dungeon.api.TypeFlag;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.entrance.Entrance;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.event.Event;
import com.littlesquad.dungeon.api.rewards.Reward;
import com.littlesquad.dungeon.api.status.Status;
import com.littlesquad.dungeon.internal.file.DungeonParser;
import com.littlesquad.dungeon.internal.file.RewardParser;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DungeonImpl extends AbstractDungeon {

    private final String id;

    private final Set<TypeFlag> flags;
    private final Status status;
    private final List<Reward> rewards;

    public DungeonImpl(final DungeonParser parser) {
        super(parser);

        flags = new HashSet<>(3);

        if (getParser().isTimeLimited()) {
            flags.add(TypeFlag.TIMED);
        }

        if (getParser().isPvP()) {
            flags.add(TypeFlag.PVP_ENABLED);
        }

        if (typeFlags().contains(TypeFlag.PVP_ENABLED))
            status = new StatusImpl(true, this);
        else status = new StatusImpl(false, this);

        id = null;

        rewards = parser.getRewards();


    }

    @Override
    public String id() {
        return null;
    }

    @Override
    public String displayName() {
        return "TestDungeon";
    }

    @Override
    public Set<TypeFlag> typeFlags() {
        return flags;
    }

    @Override
    public World getWorld() {
        return getParser().getWorld();
    }

    @Override
    public Entrance getEntrance() {
        return getParser().getEntrance();
    }

    @Override
    public Event[] getEvents() {
        return getParser().getEvents(this);
    }

    @Override
    public List<Reward> rewards() {
        return rewards;
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
        return status;
    }

    @Override
    public Checkpoint getCheckPoint(String checkPointId) {
        return null;
    }
}
