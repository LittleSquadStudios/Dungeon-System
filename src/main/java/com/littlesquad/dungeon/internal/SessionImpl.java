package com.littlesquad.dungeon.internal;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.session.AbstractDungeonSession;

import java.time.Instant;
import java.util.UUID;

public final class SessionImpl extends AbstractDungeonSession {

    SessionImpl(final UUID playerUUID, final Dungeon dungeon) {
        super(playerUUID, dungeon);
    }

    SessionImpl(final UUID playerUUID, final Dungeon dungeon, final Instant customStartTime, final int runId) {
        super(playerUUID, dungeon, customStartTime, runId);
    }
}
