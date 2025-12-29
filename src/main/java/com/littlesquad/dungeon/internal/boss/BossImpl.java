package com.littlesquad.dungeon.internal.boss;

import com.littlesquad.dungeon.api.boss.AbstractBoss;
import com.littlesquad.dungeon.api.boss.BossRoom;
import org.bukkit.Location;

public final class BossImpl extends AbstractBoss {
    private final int basePartyLevel;
    private final int multiplier;
    private final int exponent;
    private final int maxLevel;
    private final int baseLevel;
    private final String bossName;
    private final Location spawnLocation;

    public BossImpl (final BossRoom room,
                     final int basePartyLevel,
                     final int multiplier,
                     final int exponent,
                     final int maxLevel,
                     final int baseLevel,
                     final String bossName,
                     final Location spawnLocation) {
        super(room);
        this.basePartyLevel = basePartyLevel;
        this.multiplier = multiplier;
        this.exponent = exponent;
        this.maxLevel = maxLevel;
        this.baseLevel = baseLevel;
        this.bossName = bossName;
        this.spawnLocation = spawnLocation;
    }

    @Override
    public int partyLevel() {
        return basePartyLevel;
    }
    @Override
    public int multiplier() {
        return multiplier;
    }
    @Override
    public int exponent() {
        return exponent;
    }
    @Override
    public int maxLevel() {
        return maxLevel;
    }
    @Override
    public int baseLevel() {
        return baseLevel;
    }

    @Override
    public String bossName() {
        return bossName;
    }

    @Override
    public void onSpawn() {}
    @Override
    public void onDeath() {}
    @Override
    public void onDespawn() {}

    @Override
    public Location getSpawnLocation() {
        return spawnLocation;
    }
}
