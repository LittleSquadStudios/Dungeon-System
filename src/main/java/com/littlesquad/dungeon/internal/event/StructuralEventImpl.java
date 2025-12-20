package com.littlesquad.dungeon.internal.event;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.event.StructuralEvent;
import com.littlesquad.dungeon.api.event.structural.EnvironmentEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class StructuralEventImpl extends StructuralEvent {
    private final Dungeon dungeon;
    private final String id;
    private final List<String> commands;

    private final EnvironmentEvent environmentEvent;
    private final Material[] blockTypes;
    private final Location location;

    private final Set<String> events;
    private List<StructuralEvent> lazyEvents;

    private final Consumer<StructuralEvent> eventApplier;

    public StructuralEventImpl (final Dungeon dungeon,
                                final String id,
                                final List<String> commands,
                                final EnvironmentEvent environmentEvent,
                                final Material[] blockTypes,
                                final Location location,
                                final Set<String> events) {
        this.dungeon = dungeon;
        this.id = id;
        this.commands = commands;
        this.environmentEvent = environmentEvent;
        this.blockTypes = blockTypes;
        this.location = location;
        this.events = events;
        //noinspection SwitchStatementWithTooFewBranches
        eventApplier = switch (environmentEvent) {
            case ROCK_SLIDES -> e -> {

            };
            default -> _ -> {};
        };
    }

    public EnvironmentEvent getEnvironmentEvent () {
        return environmentEvent;
    }
    public Material[] getBlockTypes () {
        return blockTypes;
    }
    public Location getLocation () {
        return location;
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
        eventApplier.accept(this);
    }
    public boolean isActiveFor (final Player... players) {
        return true;
    }
}
