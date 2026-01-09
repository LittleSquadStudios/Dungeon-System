package com.littlesquad.dungeon.api.boss;

import java.util.concurrent.ScheduledFuture;

public final class PlayerBossRoomInfo {
    public final BossRoom bossRoom;
    public final ScheduledFuture<?> timeoutTask;
    public volatile ScheduledFuture<?> rewardTask;

    public PlayerBossRoomInfo (final BossRoom bossRoom,
                               final ScheduledFuture<?> timeoutTask) {
        this.bossRoom = bossRoom;
        this.timeoutTask = timeoutTask;
    }
}