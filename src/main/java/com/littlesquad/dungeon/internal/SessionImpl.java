package com.littlesquad.dungeon.internal;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.session.AbstractDungeonSession;
import com.littlesquad.dungeon.api.session.DungeonSessionManager;

import java.util.UUID;

public final class SessionImpl extends AbstractDungeonSession {

    public SessionImpl(UUID playerUUID, Dungeon dungeon) {
        super(playerUUID, dungeon);
    }

    @Override
    public DungeonSessionManager associatedDSM() {
        return null;
    }
}
