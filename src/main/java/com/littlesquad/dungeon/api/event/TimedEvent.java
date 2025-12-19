package com.littlesquad.dungeon.api.event;

import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public abstract non-sealed class TimedEvent implements Event {
    public abstract boolean isFixed ();

    public abstract long timeAmount ();
    public abstract TimeUnit timeUnit ();

    public abstract void deActiveFor (final Player... players);

    public final EventType getType () {
        return EventType.TIMED;
    }
}
