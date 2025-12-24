package com.littlesquad.dungeon.internal;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.session.AbstractSessionManager;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.api.status.Status;

import java.util.UUID;
import java.util.function.Consumer;

public final class SessionManager extends AbstractSessionManager {

    private final Dungeon dungeonRef;

    public SessionManager(final Dungeon dungeon) {
        this.dungeonRef = dungeon;
    }

    public static DungeonSession getDungeonSession(final UUID playerId) {
        return sessions.get(playerId);
    }

    @Override
    public Status associatedStatus() {
        return null;
    }

    @Override
    public void onReload() {

    }

    @Override
    public void recoverActiveSessions(Consumer<UUID> onExpire) {

    }

    @Override
    public DungeonSession createSessionInstance(UUID playerId) {
        return new SessionImpl(playerId, dungeonRef);
    }

}
