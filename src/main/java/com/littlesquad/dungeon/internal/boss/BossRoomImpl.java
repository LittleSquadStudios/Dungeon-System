package com.littlesquad.dungeon.internal.boss;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.boss.AbstractBossRoom;
import com.littlesquad.dungeon.api.boss.Boss;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.rewards.Reward;
import org.bukkit.Location;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class BossRoomImpl extends AbstractBossRoom {
    private final String id;
    private final int capacity;
    private final boolean onePartyOnly;
    private final List<String> accessDeniedCommands;
    private final String fallbackBossRoomID;
    private BossRoom lazyFallback;
    private final List<String> enqueuingCommands;
    private final long maxBossFightDurationTime;
    private final TimeUnit maxBossFightDurationUnit;
    private final List<String> timedOutCommands;
    private final long kickAfterCompletionTime;
    private final TimeUnit kickAfterCompletionUnit;
    private final Boss boss;
    private final List<Reward> rewards;

    public BossRoomImpl (final Dungeon dungeon,
                         final String id,
                         final int capacity,
                         final boolean onePartyOnly,
                         final List<String> accessDeniedCommands,
                         final String fallbackBossRoomID,
                         final List<String> enqueuingCommands,
                         final long maxBossFightDurationTime,
                         final TimeUnit maxBossFightDurationUnit,
                         final List<String> timedOutCommands,
                         final long kickAfterCompletionTime,
                         final TimeUnit kickAfterCompletionUnit,
                         final int basePartyLevel,
                         final int multiplier,
                         final int exponent,
                         final int maxLevel,
                         final int baseLevel,
                         final String bossName,
                         final Location spawnLocation,
                         final List<Reward> rewards) {
        super(dungeon);
        this.id = id;
        this.capacity = capacity;
        this.onePartyOnly = onePartyOnly;
        this.accessDeniedCommands = accessDeniedCommands;
        this.fallbackBossRoomID = fallbackBossRoomID;
        this.enqueuingCommands = enqueuingCommands;
        this.maxBossFightDurationTime = maxBossFightDurationTime;
        this.maxBossFightDurationUnit = maxBossFightDurationUnit;
        this.timedOutCommands = timedOutCommands;
        this.kickAfterCompletionTime = kickAfterCompletionTime;
        this.kickAfterCompletionUnit = kickAfterCompletionUnit;
        this.rewards = rewards;
        this.boss = new BossImpl(
                this,
                basePartyLevel,
                multiplier,
                exponent,
                maxLevel,
                baseLevel,
                bossName,
                spawnLocation);
        BossRoomManager.getInstance().register(id, this);
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public boolean onePartyOnly() {
        return onePartyOnly;
    }

    @Override
    public List<String> accessDeniedCommands() {
        return accessDeniedCommands;
    }

    @Override
    public BossRoom fallback() {
        return fallbackBossRoomID.isEmpty()
                ? null
                : lazyFallback != null
                ? lazyFallback
                : (lazyFallback = BossRoomManager.getInstance().get(fallbackBossRoomID));
    }

    @Override
    public List<String> enqueuingCommands() {
        return enqueuingCommands;
    }

    public long maxBossFightDurationTime () {
        return maxBossFightDurationTime;
    }
    public TimeUnit maxBossFightDurationUnit () {
        return maxBossFightDurationUnit;
    }

    public List<String> timedOutCommands () {
        return timedOutCommands;
    }

    public long kickAfterCompletionTime () {
        return kickAfterCompletionTime;
    }
    public TimeUnit kickAfterCompletionUnit () {
        return kickAfterCompletionUnit;
    }

    @Override
    public Boss getBoss() {
        return boss;
    }

    @Override
    public List<Reward> rewards() {
        return rewards;
    }
}
