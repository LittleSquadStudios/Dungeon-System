package com.littlesquad.dungeon.api.boss;

import io.lumine.mythic.api.mobs.MythicMob;
import org.bukkit.Location;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface Boss {

    /**
     * Returns the boss name in this boss room.
     *
     * @return {@link String} representing the boss name
     * @since 1.0.0
     */
    String bossName();

    /**
     * Returns the base level of the boss.
     * @return base level
     */
    int baseLevel();

    /**
     * This main method handles the spawning
     * logic of the boss.
     *
     * @since 1.0.0
     * @author LittleSquad
     * */
    void spawn();

    /**
     * Returns the current state of the boss.
     * <p>
     * The boss state represents the current lifecycle phase of the boss
     * (for example: not spawned, alive, dead, or despawned).
     * </p>
     *
     * <p>
     * This information is especially useful for managing the lifecycle of
     * a {@link BossRoom}, such as determining when the room should be reset.
     * For instance, after the boss is slain, the plugin can check the boss
     * state to safely restore the boss room and prepare it for the next run.
     * </p>
     *
     * @return the current {@link BossState} of the boss
     * @since 1.0.0
     * @author LittleSquad
     */
    BossState getState();


    boolean isAlive();

    Optional<MythicMob> getSpawnedEntity();

    void onSpawn();
    void onDeath();
    void onDespawn();

    /**
     * This method will return the players who have participated in the fight
     * <p>
     * A player is considered a participant if they have successfully inflicted
     * at least one hit to the boss during the encounter
     * </p>
     *
     * @return A {@link Set} of {@link UUID} who hit the boss
     * @since 1.0.0
     * @author LittleSquad
     * */
    Set<UUID> getParticipants();

    Location getSpawnLocation();
}
