package com.littlesquad.dungeon.api.boss;

public abstract class AbstractBossRoom implements BossRoom {

    @Override
    public double calculateBossLevel() {
        return Math.max(maxLevel(), Math.min(baseLevel(), Math.pow(baseLevel() + (getRealPartyLevel() - partyLevel()) * multiplier(), exponent())));
    }

    /**
     * Returns the base level of the boss.
     * @return base level
     */
    public abstract int baseLevel();

    /**
     * Returns the reference party level for scaling.
     * @return party level
     */
    public abstract int partyLevel();

    /**
     * Returns the multiplier for the boss level calculation.
     * @return multiplier
     */
    public abstract int multiplier();

    /**
     * Returns the exponent for non-linear scaling in boss level calculation.
     * @return exponent
     */
    public abstract int exponent();

    /**
     * Returns the maximum level the boss can reach.
     * @return max level
     */
    public abstract int maxLevel();

    /**
     * Returns the actual sum of player levels in the current party.
     * Must be implemented by concrete class to provide the real party level.
     * @return real party level
     */
    protected abstract int getRealPartyLevel();
}
