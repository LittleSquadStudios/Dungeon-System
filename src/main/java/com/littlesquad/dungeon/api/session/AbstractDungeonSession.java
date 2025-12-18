package com.littlesquad.dungeon.api.session;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public abstract class AbstractDungeonSession implements DungeonSession {

    private final ScheduledExecutorService ex;

    public AbstractDungeonSession() {
        ex = new ScheduledThreadPoolExecutor(5);
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onEnd() {

    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public long timeIn() {
        return 0;
    }

    @Override
    public Long runId() {
        return 0L;
    }

}
