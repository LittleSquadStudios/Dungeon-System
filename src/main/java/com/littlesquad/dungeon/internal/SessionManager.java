package com.littlesquad.dungeon.internal;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.api.status.Status;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class SessionManager {
    private static final SessionManager manager = new SessionManager();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return manager;
    }

    private final ConcurrentHashMap<UUID, DungeonSession> sessions = new ConcurrentHashMap<>();
    private final Map<Dungeon, List<DungeonSession>> dungeonSessions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public void startSession(final Dungeon dungeon, UUID playerId) {
        final DungeonSession session = createSessionInstance(dungeon, playerId);
        sessions.put(playerId, session);
        dungeonSessions.computeIfAbsent(dungeon, _ -> new CopyOnWriteArrayList<>())
                .add(session);
    }

    public void startTimedSession(final Dungeon dungeon,
                                  UUID playerId,
                                  long duration,
                                  TimeUnit unit,
                                  Consumer<UUID> onExpire) {

        final DungeonSession session = createSessionInstance(dungeon, playerId);
        sessions.put(playerId, session);
        dungeonSessions.computeIfAbsent(dungeon, _ -> new CopyOnWriteArrayList<>())
                .add(session);

        scheduler.schedule(() -> {
            if (session.isActive()) {
                onExpire.accept(playerId);
                endSession(playerId, ExitReason.TIME_EXPIRED);
            }
        }, duration, unit);

    }

    public void endSession(UUID playerId, ExitReason exitReason) {

        final DungeonSession session = sessions.remove(playerId);
        if (session != null
                && dungeonSessions
                .get(session.getDungeon())
                .remove(session)) {
            session.stopSession();
        }

    }

    public DungeonSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public List<DungeonSession> getSessions() {
        return List.copyOf(sessions.values());
    }

    public List<DungeonSession> getDungeonSessions (final Dungeon dungeon) {
        return dungeonSessions.getOrDefault(dungeon, Collections.emptyList());
    }

    public void shutdown() {
        dungeonSessions.clear();
        sessions.values().forEach(DungeonSession::stopSession);
        sessions.clear();
        scheduler.shutdown();
    }

    public void onReload() {

    }

    public void recoverActiveSessions(Consumer<UUID> onExpire) {

    }


    public DungeonSession createSessionInstance(final Dungeon dungeon, UUID playerId) {
        return new SessionImpl(playerId, dungeon);
    }

}
