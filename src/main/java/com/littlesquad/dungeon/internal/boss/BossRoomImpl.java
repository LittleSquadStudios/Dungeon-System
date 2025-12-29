package com.littlesquad.dungeon.internal.boss;

import com.littlesquad.dungeon.api.boss.AbstractBossRoom;
import com.littlesquad.dungeon.api.boss.Boss;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.rewards.Reward;
import org.bukkit.Location;

import java.util.List;

public final class BossRoomImpl extends AbstractBossRoom {
    private final String id;
    private final int capacity;
    private final boolean onePartyOnly;
    private final List<String> accessDeniedCommands;
    private final String fallbackBossRoomID;
    private BossRoom lazyFallback;
    private final List<String> enqueuingCommands;
    private final Boss boss;
    private final List<Reward> rewards;

    public BossRoomImpl (final String id,
                         final int capacity,
                         final boolean onePartyOnly,
                         final List<String> accessDeniedCommands,
                         final String fallbackBossRoomID,
                         final List<String> enqueuingCommands,
                         final int basePartyLevel,
                         final int multiplier,
                         final int exponent,
                         final int maxLevel,
                         final int baseLevel,
                         final String bossName,
                         final Location spawnLocation,
                         final List<Reward> rewards) {
        super();
        this.id = id;
        this.capacity = capacity;
        this.onePartyOnly = onePartyOnly;
        this.accessDeniedCommands = accessDeniedCommands;
        this.fallbackBossRoomID = fallbackBossRoomID;
        this.enqueuingCommands = enqueuingCommands;
        this.boss = new BossImpl(
                this,
                basePartyLevel,
                multiplier,
                exponent,
                maxLevel,
                baseLevel,
                bossName,
                spawnLocation);
        this.rewards = rewards;
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

    @Override
    public Boss getBoss() {
        return boss;
    }

    @Override
    public List<Reward> rewards() {
        return rewards;
    }
}
