package com.littlesquad.dungeon.api.session;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.entrance.ExitReason;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a runtime session for a player inside a dungeon.
 * <p>
 * A {@code DungeonSession} is created when a player enters a dungeon and
 * remains active until the player leaves or the dungeon run ends.
 * Each session is uniquely associated with a single player and is responsible for:
 * <ul>
 *     <li>Tracking the time spent inside the dungeon</li>
 *     <li>Providing timing data for scoreboards or UI elements</li>
 *     <li>Tracking combat statistics such as kills and damage dealt</li>
 *     <li>Identifying the dungeon run the player belongs to</li>
 * </ul>
 * <p>
 * There is exactly <b>one active session per player</b> at any given time.
 *
 * @since 1.0.0
 * @author LittleSquad
 */
public interface DungeonSession {

    Dungeon getDungeon ();

    /**
     * Returns the unique identifier of the player associated with this session.
     *
     * @return the player's {@link UUID}
     *
     * @since 1.0.0
     * @author LittleSquad
     */
    UUID playerId();

    /**
     * Called when the session ends.
     * <p>
     * This method should stop timers, finalize statistics and
     * mark the session as inactive.
     * It is typically invoked when the player leaves the dungeon
     * or when the dungeon run finishes.
     *
     * @since 1.0.0
     * @author LittleSquad
     */
    void stopSession(final ExitReason reason);

    /**
     * Indicates whether this session is currently active.
     *
     * @return {@code true} if the session is active, {@code false} otherwise
     *
     * @since 1.0.0
     * @author LittleSquad
     */
    boolean isActive();

    /**
     * Returns the amount of time the player has spent in the dungeon.
     * <p>
     * If the session is active, the returned value represents the elapsed time
     * since constructor was called.
     * If the session has ended, it represents the total duration of the session.
     *
     * @return the time spent in the dungeon, in the time unit specified
     *
     * @since 1.0.0
     * @author LittleSquad
     */
    long timeIn();

    Instant enterTime();

    Instant exitTime();

    /**
     * Returns the total number of kills made by the player
     * during this dungeon session.
     *
     * @return the kill count
     *
     * @since 1.0.0
     * @author LittleSquad
     */
    int kills();

    /**
     * Returns the total amount of damage dealt by the player
     * during this dungeon session.
     *
     * @return the total damage dealt
     *
     * @since 1.0.0
     * @author LittleSquad
     */
    double damageDealt();

    double damageTaken();

    /**
     * Adds the specified amount of kills to the current kill counter.
     *
     * @param kill the number of kills to add
     *
     * @since 1.0.0
     * @author LittleSquad
     */
    void addKill(final int kill);

    void addDeath();

    int deaths();

    /**
     * Adds the specified amount of damage to the total damage dealt.
     *
     * @param damage the damage amount to add
     *
     * @since 1.0.0
     * @author LittleSquad
     */
    void addDamage(final double damage);

    void addDamageTaken(double damage);
}
