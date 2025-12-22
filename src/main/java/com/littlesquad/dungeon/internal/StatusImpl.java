package com.littlesquad.dungeon.internal;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.session.DungeonSessionManager;
import com.littlesquad.dungeon.api.status.AbstractStatus;

import java.util.List;

public final class StatusImpl extends AbstractStatus {

    private final Dungeon dungeonRef;

    public StatusImpl(boolean isPvp, final Dungeon dungeon) {
        super(isPvp);
        this.dungeonRef = dungeon;
    }

    @Override
    public Dungeon associatedDungeon() {
        return null;
    }

    @Override
    public List<BossRoom> bossRooms() {
        return List.of();
    }

    @Override
    public DungeonSessionManager sessionManager() {
        return new SessionManagerImpl(dungeonRef);
    }
}
