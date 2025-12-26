package com.littlesquad.dungeon.internal;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.session.AbstractDungeonSession;

import java.util.UUID;

public final class SessionImpl extends AbstractDungeonSession {

    SessionImpl(UUID playerUUID, Dungeon dungeon) {
        super(playerUUID, dungeon);
    }
}
