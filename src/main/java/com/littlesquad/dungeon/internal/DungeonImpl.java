package com.littlesquad.dungeon.internal;

import com.littlesquad.dungeon.api.AbstractDungeon;
import com.littlesquad.dungeon.api.TypeFlag;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.entrance.Entrance;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.event.Event;
import com.littlesquad.dungeon.api.event.EventType;
import com.littlesquad.dungeon.api.rewards.Reward;
import com.littlesquad.dungeon.api.status.Status;
import com.littlesquad.dungeon.internal.file.DungeonParser;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DungeonImpl extends AbstractDungeon {

    private final String id;
    private final String displayName;
    private final World world;
    private final Entrance entrance;

    private final Set<TypeFlag> flags;
    private final Status status;
    private final List<Reward> rewards;
    private final Event[] events;
    private final Map<String, Event> eventMap;
    private final Checkpoint[] checkpoints;
    private final BossRoom[] bossrooms;

    public DungeonImpl(final DungeonParser parser) {
        super(parser);

        this.id = parser.getId();
        this.displayName = parser.displayName();
        this.world = parser.getWorld();
        this.entrance = parser.getEntrance();

        this.flags = new HashSet<>(3);

        if (parser.isTimeLimited()) {
            flags.add(TypeFlag.TIMED);
        }

        if (parser.isPvP()) {
            flags.add(TypeFlag.PVP_ENABLED);
        }

        this.status = new StatusImpl(flags.contains(TypeFlag.PVP_ENABLED), this);
        this.rewards = parser.getRewards();
        this.events = parser.getEvents(this);
        eventMap = new ConcurrentHashMap<>(events.length);
        for (final Event event : events) {
            eventMap.put(event.getID(), event);
            if (event.getType() == EventType.STRUCTURAL)
                event.triggerActivation();
        }
        this.checkpoints = parser.getCheckpoints(this);

        bossrooms = parser.getBossRooms(this);

    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public Set<TypeFlag> typeFlags() {
        return Collections.unmodifiableSet(flags);
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public Entrance getEntrance() {
        return entrance;
    }

    @Override
    public Event[] getEvents() {
        return events;
    }

    @Override
    public Event getEvent (final String id) {
        return eventMap.get(id);
    }

    @Override
    public List<Reward> rewards() {
        return rewards;
    }

    @Override
    public Checkpoint[] getCheckpoints() {
        return checkpoints;
    }

    @Override
    public BossRoom[] getBossRooms() {
        return bossrooms;
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