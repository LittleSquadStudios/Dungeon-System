package com.littlesquad.dungeon.api.session;

import java.util.UUID;

public abstract class AbstractTimedDungeonSession extends AbstractDungeonSession {


    public AbstractTimedDungeonSession(UUID player) {
        super(player, null);
    }
}
