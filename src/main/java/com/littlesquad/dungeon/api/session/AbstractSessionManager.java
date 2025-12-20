package com.littlesquad.dungeon.api.session;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.status.Status;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class AbstractSessionManager implements DungeonSessionManager {

    private final ConcurrentHashMap<UUID, DungeonSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public void startSession(UUID playerId, TimeUnit unit) {
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
