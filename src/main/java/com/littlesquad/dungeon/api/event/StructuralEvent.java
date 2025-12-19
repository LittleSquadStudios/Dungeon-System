package com.littlesquad.dungeon.api.event;

import com.littlesquad.dungeon.api.event.structural.EnvironmentEvent;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;

public abstract non-sealed class StructuralEvent implements Event {

    public abstract EnvironmentEvent getEnvironmentEvent ();

    public abstract Material[] getBlockTypes ();

    public abstract Location getLocation ();

    public abstract List<StructuralEvent> conditionedBy ();

    public final EventType getType () {
        return EventType.STRUCTURAL;
    }
}
