package com.littlesquad.dungeon.api.event.requirement;

import org.bukkit.event.Event;

@FunctionalInterface
public interface Requirements {
    boolean check (final RequirementType type, final Event event);
}
