package com.littlesquad.dungeon.api.session;

import com.littlesquad.dungeon.api.entrance.ExitReason;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

public abstract class AbstractSessionManager implements DungeonSessionManager {

    protected static final ConcurrentHashMap<UUID, DungeonSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public void startSession(UUID playerId) {
        final DungeonSession session = createSessionInstance(playerId);
        sessions.put(playerId, session);
    }

    @Override
    public void startTimedSession(UUID playerId,
                                  long duration,
                                  TimeUnit unit,
                                  Consumer<UUID> onExpire) {

        final DungeonSession session = createSessionInstance(playerId);
        sessions.put(playerId, session);

        scheduler.schedule(() -> {
            if (session.isActive()) {
                onExpire.accept(playerId);
                endSession(playerId, ExitReason.TIME_EXPIRED);
            }
        }, duration, unit);

    }

    @Override
    public void endSession(UUID playerId, ExitReason exitReason) {

        final DungeonSession session = sessions.remove(playerId);
        if (session != null) {
            session.stopSession();
        }

    }

    @Override
    public DungeonSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    @Override
    public List<DungeonSession> getSessions() {
        return sessions.values().stream().toList();
    }

    @Override
    public void shutdown() {
        sessions.values().forEach(DungeonSession::stopSession);
        sessions.clear();
        scheduler.shutdown();
    }
}
