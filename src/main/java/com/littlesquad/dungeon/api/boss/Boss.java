package com.littlesquad.dungeon.api.boss;

public abstract class Boss {

    /**
     * Calculates the level of the boss based on party information
     * and configuration parameters. In our basic implementation
     * {@link Boss}, we strictly follow what's defined in
     * <code>dungeon.yml</code> under the section <code>boss-rooms</code>.
     *
     * <p>
     * The formula used to calculate the final boss level is:
     * </p>
     *
     * <pre>{@code
     * final_level = Max(
     *     max_level,
     *     Min(
     *         base_level,
     *         ((base_level + (real_party_level - party_level)) * multiplier) ^ exponent
     *     )
     * )
     * }</pre>
     *
     * <p>
     * Where:
     * <ul>
     *   <li><b>base_level</b> – the default level of the boss.</li>
     *   <li><b>party_level</b> – the reference party level defined in the YAML.</li>
     *   <li><b>real_party_level</b> – the sum of the actual levels of the players in the party.</li>
     *   <li><b>multiplier</b> – scaling factor for the level adjustment.</li>
     *   <li><b>exponent</b> – exponent for non-linear scaling.</li>
     *   <li><b>max_level</b> – the maximum allowed level of the boss.</li>
     * </ul>
     * </p>
     *
     * <p>
     * The formula works as follows:
     * <ol>
     *   <li>Compute the difference between the real party level and the reference party level:
     *       <code>delta = real_party_level - party_level</code>.</li>
     *   <li>Add this difference to the base level and apply the multiplier:
     *       <code>adjusted = (base_level + delta) * multiplier</code>.</li>
     *   <li>Raise to the exponent to allow non-linear scaling:
     *       <code>scaled = adjusted ^ exponent</code>.</li>
     *   <li>Take the minimum between the base level and the scaled value, ensuring the boss does not drop below the base level.</li>
     *   <li>Finally, take the maximum between <code>max_level</code> and the previous result to ensure the boss does not exceed the maximum allowed level.</li>
     * </ol>
     * </p>
     *
     * @return {@link Integer} representing the final boss level
     * @since 1.0.0
     */
    public double calculateBossLevel (final double actualPartyLevel) {
        return Math.max(maxLevel(), Math.min(baseLevel(), Math.pow(baseLevel() + (actualPartyLevel - partyLevel()) * multiplier(), exponent())));
    }

    /**
     * Returns the boss name in this boss room.
     *
     * @return {@link String} representing the boss name
     * @since 1.0.0
     */
    public abstract String bossName();

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
}
