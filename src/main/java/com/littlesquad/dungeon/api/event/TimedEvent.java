package com.littlesquad.dungeon.api.event;

import java.util.concurrent.TimeUnit;

public abstract non-sealed class TimedEvent implements Event {
    public abstract boolean isFixed ();

    public abstract long timeAmount ();
    public abstract TimeUnit timeUnit ();

    public final EventType getType () {
        return EventType.TIMED;
    }
}
