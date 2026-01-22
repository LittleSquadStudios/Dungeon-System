package com.littlesquad.dungeon.internal.event;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.event.StructuralEvent;
import com.littlesquad.dungeon.api.event.structural.EnvironmentEvent;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.internal.SessionManager;
import com.littlesquad.dungeon.internal.utils.CommandUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public final class StructuralEventImpl extends StructuralEvent {
    private final Dungeon dungeon;
    private final String id;
    private final List<String> commands;

    private final EnvironmentEvent environmentEvent;
    private final Material[] blockTypes;
    private final Location[] locations;

    private final Set<String> events;
    private List<StructuralEvent> lazyEvents;

    private final Runnable eventApplier;

    private static final Lock globalStateLock = new ReentrantLock(false);
    /* State values:
     * [-1]: active
     * [0]: non-active / ready-for-activation
     */
    private int state;
    private Runnable eventDeactivator = () -> {};

    public StructuralEventImpl (final Dungeon dungeon,
                                final String id,
                                final List<String> commands,
                                final EnvironmentEvent environmentEvent,
                                final Material[] blockTypes,
                                final Location[] locations,
                                final Set<String> events,
                                final long minRetryTime,
                                final long maxRetryTime,
                                final TimeUnit retryUnit,
                                final long minDeactivationTime,
                                final long maxDeactivationTime,
                                final TimeUnit deactivationUnit) {
        this.dungeon = dungeon;
        this.id = id;
        this.commands = commands;
        this.environmentEvent = environmentEvent;
        this.blockTypes = blockTypes;
        this.locations = locations;
        this.events = events;
        eventApplier = switch (environmentEvent) {
            case ROCK_SLIDES -> {
                final List<Block> airBlockList = new ArrayList<>(Math
                        .abs(locations[0].getBlockX() - locations[1].getBlockX()) * Math
                        .abs(locations[0].getBlockY() - locations[1].getBlockY()) * Math
                        .abs(locations[0].getBlockZ() - locations[1].getBlockZ()));
                final int toX = Math.max(locations[0].getBlockX(), locations[1].getBlockX()),
                        toY = Math.max(locations[0].getBlockY(), locations[1].getBlockY()),
                        toZ = Math.max(locations[0].getBlockZ(), locations[1].getBlockZ());
                for (int x = Math.min(locations[0].getBlockX(), locations[1].getBlockX()); x <= toX; ++x)
                    for (int y = Math.min(locations[0].getBlockY(), locations[1].getBlockY()); y <= toY; ++y)
                        for (int z = Math.min(locations[0].getBlockZ(), locations[1].getBlockZ()); z <= toZ; ++z)
                            airBlockList.add(dungeon.getWorld().getBlockAt(x, y, z));
                yield () -> {
                    try {
                        globalStateLock.lock();
                        if (state == 0) {
                            if (conditionedBy()
                                .stream()
                                .allMatch(Predicate.not(StructuralEvent::isActiveFor))) {
                                final ScheduledFuture<?> deactivationTask = Main
                                        .getScheduledExecutor()
                                        .schedule(() -> eventDeactivator.run(),
                                                ThreadLocalRandom.current().nextLong(
                                                        minDeactivationTime,
                                                        maxDeactivationTime + 1),
                                                deactivationUnit);
                                state = -1;
                                eventDeactivator = new Runnable() {
                                    public void run () {
                                        try {
                                            globalStateLock.lock();
                                            if (eventDeactivator == this) {
                                                deactivationTask.cancel(false);

                                                //TODO: Cool disappear effects for non-air blocks in 'airBlockList'

                                                state = 0;
                                                eventDeactivator = () -> {};
                                            }
                                        } finally {
                                            globalStateLock.unlock();
                                        }
                                    }
                                };
                                CommandUtils.executeMultiForMulti(
                                        Bukkit.getConsoleSender(),
                                        commands,
                                        SessionManager.getInstance()
                                                .getDungeonSessions(dungeon)
                                                .parallelStream()
                                                .map(DungeonSession::playerId)
                                                .map(Bukkit::getPlayer)
                                                .filter(Objects::nonNull)
                                                .toArray(Player[]::new));

                                //TODO: Make blocks appear

                            } else {
                                final ScheduledFuture<?> retryTask = Main
                                        .getScheduledExecutor()
                                        .schedule(() -> triggerActivation(),
                                                ThreadLocalRandom.current().nextLong(
                                                        minRetryTime,
                                                        maxRetryTime + 1),
                                                retryUnit);
                                eventDeactivator = new Runnable() {
                                    public void run () {
                                        try {
                                            globalStateLock.lock();
                                            if (eventDeactivator == this)
                                                retryTask.cancel(false);
                                        } finally {
                                            globalStateLock.unlock();
                                        }
                                    }
                                };
                            }
                        }
                    } finally {
                        globalStateLock.unlock();
                    }
                };
            }
            case NONE -> () -> {
                try {
                    globalStateLock.lock();
                    if (state == 0) {
                        if (conditionedBy()
                                .stream()
                                .allMatch(Predicate.not(StructuralEvent::isActiveFor))) {
                            final ScheduledFuture<?> deactivationTask = Main
                                    .getScheduledExecutor()
                                    .schedule(() -> eventDeactivator.run(),
                                            ThreadLocalRandom.current().nextLong(
                                                    minDeactivationTime,
                                                    maxDeactivationTime + 1),
                                            deactivationUnit);
                            state = -1;
                            eventDeactivator = new Runnable() {
                                public void run () {
                                    try {
                                        globalStateLock.lock();
                                        if (eventDeactivator == this) {
                                            deactivationTask.cancel(false);
                                            state = 0;
                                            eventDeactivator = () -> {};
                                        }
                                    } finally {
                                        globalStateLock.unlock();
                                    }
                                }
                            };
                            CommandUtils.executeMultiForMulti(
                                    Bukkit.getConsoleSender(),
                                    commands,
                                    SessionManager.getInstance()
                                            .getDungeonSessions(dungeon)
                                            .parallelStream()
                                            .map(DungeonSession::playerId)
                                            .map(Bukkit::getPlayer)
                                            .filter(Objects::nonNull)
                                            .toArray(Player[]::new));
                        } else {
                            final ScheduledFuture<?> retryTask = Main
                                    .getScheduledExecutor()
                                    .schedule(() -> triggerActivation(),
                                            ThreadLocalRandom.current().nextLong(
                                                    minRetryTime,
                                                    maxRetryTime + 1),
                                            retryUnit);
                            eventDeactivator = new Runnable() {
                                public void run () {
                                    try {
                                        globalStateLock.lock();
                                        if (eventDeactivator == this)
                                            retryTask.cancel(false);
                                    } finally {
                                        globalStateLock.unlock();
                                    }
                                }
                            };
                        }
                    }
                } finally {
                    globalStateLock.unlock();
                }
            };
        };
    }

    public EnvironmentEvent getEnvironmentEvent () {
        return environmentEvent;
    }
    public Material[] getBlockTypes () {
        return blockTypes;
    }
    public Location[] getLocations () {
        return locations;
    }
    public List<StructuralEvent> conditionedBy () {
        return lazyEvents != null
                ? lazyEvents
                : (lazyEvents
                = Arrays
                .stream(dungeon.getEvents())
                .parallel()
                .filter(e -> e instanceof StructuralEvent
                        && events.contains(e.getID()))
                .map(e -> (StructuralEvent) e)
                .toList());
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

    public void triggerActivation (final Player... emptyAndIgnored) {
        eventApplier.run();
    }
    public boolean isActiveFor (final Player... emptyAndIgnored) {
        return state < 0;
    }

    public void close () {
        try {
            globalStateLock.lock();
            eventDeactivator.run();
        } finally {
            globalStateLock.unlock();
        }
    }
}
