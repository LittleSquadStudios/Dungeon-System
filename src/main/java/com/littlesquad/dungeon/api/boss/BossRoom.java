package com.littlesquad.dungeon.api.boss;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Represents a single instance of a BossRoom inside a dungeon.
 * <p>
 * A BossRoom defines the environment where a boss fight occurs, including:
 * <ul>
 *     <li>The boss itself that is a MythicMob</li>
 *     <li>The maximum number of players allowed inside the boss room at a time.</li>
 *     <li>Whether only one party is allowed at a time, enforcing exclusive access.</li>
 *     <li>Access restrictions, including commands to execute when entry is denied.</li>
 *     <li>The boss scaling parameters (base level, party level, multiplier, exponent, max level), which are read from the <code>boss-rooms</code> section in <code>dungeon.yml</code> and used to calculate the final boss level. Better defined in {@link Boss} we don't put here that params since you may need to create an implementation that doesn't need any of those params</li>
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
     * {@link Boss} implementation is more strict and evaluates party
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

    Boss getBoss ();

    List<String> rewards ();
}
