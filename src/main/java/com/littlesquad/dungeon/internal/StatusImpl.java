package com.littlesquad.dungeon.internal;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.status.AbstractStatus;
import org.bukkit.Bukkit;

import java.util.List;

public final class StatusImpl extends AbstractStatus {
    public StatusImpl(boolean isPvp, final Dungeon dungeon) {
        super(isPvp, dungeon);
    }

    @Override
    public Dungeon associatedDungeon() {
        return null;
    }

    @Override
    public List<BossRoom> bossRooms() {
        return List.of();
    }
}
