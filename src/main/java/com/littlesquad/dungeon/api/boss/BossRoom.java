package com.littlesquad.dungeon.api.boss;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Represents a single instance of a BossRoom inside a dungeon.
 * <p>
 * A BossRoom defines the environment where a boss fight occurs, including:
 * <ul>
 *     <li>The boss itself, which can be either a normal mob or a MythicMob.
 *         If using MythicMobs, the boss name must be prefixed with <code>mythicmob:</code> (e.g., <code>mythicmob:BOSS_NAME</code>).</li>
 *     <li>The maximum number of players allowed inside the boss room at a time.</li>
 *     <li>Whether only one party is allowed at a time, enforcing exclusive access.</li>
 *     <li>Access restrictions, including commands to execute when entry is denied.</li>
 *     <li>The boss scaling parameters (base level, party level, multiplier, exponent, max level), which are read from the <code>boss-rooms</code> section in <code>dungeon.yml</code> and used to calculate the final boss level. Better defined in {@link AbstractBossRoom} we don't put here that params since you may need to create an implementation that doesn't need any of those params</li>
 *     <li>Rewards for players upon defeating the boss, which may include items, experience points, loot chests, or custom commands.</li>
 *     <li>The location of the boss room, which is used to spawn the boss, teleport players, or trigger environmental effects.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This interface allows for flexible implementations. For example,
 * a custom implementation may provide more complex logic for boss level scaling,
 * dynamic rewards, or integration with other dungeon systems.
 * </p>
 *
 * @since 1.0.0
 * @author LittleSquad
 */
public interface BossRoom {

    /**
     * Attempts to allow a player (or their party) to join this boss room.
     * <p>
     * The implementation of this method should enforce all relevant rules for entry:
     * <ul>
     *     <li>Check the current room capacity to prevent exceeding the maximum allowed players.</li>
     *     <li>Verify if only a single party is allowed inside the room and block additional parties if necessary.</li>
     *     <li>Execute any configured access-denied commands if the join attempt is rejected.</li>
     *     <li>Perform any necessary actions upon successful entry, such as teleporting the player, applying buffs, or triggering environmental effects.</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>Important:</b> This method differs from
     * {@link com.littlesquad.dungeon.api.Dungeon#tryEnter(Player)} in that it does
     * <i>not</i> automatically enforce party-related checks. In contrast, the
     * {@link AbstractBossRoom} implementation is more strict and evaluates party
     * membership and party-level requirements before allowing entry.
     * </p>
     *
     * <p>
     * Use this method when you want to give a player the opportunity to join a boss
     * room independently of party restrictions, for example in custom implementations
     * or single-player scenarios.
     * </p>
     *
     * @param playerId the {@link UUID} of the player attempting to join the boss room
     * @return {@code true} if the player (or their party) successfully joined the room;
     *         {@code false} if the join attempt was denied due to room rules
     * @since 1.0.0
     */
    boolean join(UUID playerId);

    void join(final Player... players);

    /**
     * Returns the maximum number of players that can enter this boss room.
     *
     * @return {@link Integer} representing the room capacity
     * @since 1.0.0
     */
    int capacity();

    /**
     * Indicates if only one party can be inside this boss room at a time.
     *
     * @return {@link Boolean} true if only one party is allowed, false otherwise
     * @since 1.0.0
     */
    boolean onePartyOnly();

    /**
     * Returns a list of commands to be executed if a player or party
     * is denied access to the boss room.
     *
     * @return {@link List} of command strings
     * @since 1.0.0
     */
    List<String> accessDeniedCommands();

    /**
     * Returns the boss name in this boss room.
     * <p>
     * If using a MythicMob, prefix the name with <code>mythicmob:</code>,
     * e.g., <code>mythicmob:BOSS_NAME</code>.
     * </p>
     *
     * @return {@link String} representing the boss name
     * @since 1.0.0
     */
    String bossName();

    /**
     * Calculates the level of the boss based on party information
     * and configuration parameters. In our basic implementation
     * {@link AbstractBossRoom}, we strictly follow what's defined in
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
    double calculateBossLevel();

    //TODO: Tocca fa l'astrazione dei rewards

}
