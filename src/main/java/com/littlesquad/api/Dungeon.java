package com.littlesquad.api;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;

public interface Dungeon {

    /**
     * It's taken by checking the .yml name
     * and is matched in a map with the dungeon
     * instance itself
     * @return {@link String} Identifier of the dungeon
     * @since 1.0.0
     * @author LittleSquad
    **/
    String id();

    /**
     * A bit different from id since it contains
     * formatted name of the dungeon, that is shown
     * for example by an action bar or scoreboard
     * @return {@link String} Formatted name of the dungeon
     * @since 1.0.0
     * @author LittleSquad
     **/
    String displayName();

    /**
     * This method return a set containing a {@link TypeFlag flag} that<br>
     * identifies the options for this dungeon, for example if in a configuration like this <pre> {@code
     *  pvp: false
     *
     *  time:
     *    limited: true
     *    amount: 5
     *    unit: MINUTES
     * } </pre><br>
     * Both {@link TypeFlag TIMED, PVP_ENABLED} will be enabled and if
     * bossroom field is compiled it will contain also <b>HAS_BOSSROOM</b>
     * @return the {@link Set} containing the {@link TypeFlag enabled types}
     * @since 1.0.0
     * @author LittleSquad
     *
     *
     * */
    Set<TypeFlag> typeFlags();

    /**
     * This method tries to teleport a player into a dungeon
     * checking information and executing operations such as:
     * <ul>
     *   <li>Current occupied slots.</li>
     *   <li>Player permissions verification for bypassing slot limits (if applicable).</li>
     *   <li>Party requirement (if the dungeon requires a party).</li>
     *   <li>Validate minimum level for the player and for the whole party.</li>
     *   <li>Execute fallback commands if conditions are met (e.g. teleport to fallback location).</li>
     *   <li>Return <code>true</code> if all checks pass and the teleport is performed; otherwise return <code>false</code>.</li>
     * </ul>
     * @return {@link EntryResponse}
     * @since 1.0.0
     * @author LittleSquad
     *
     *
     * */
    EntryResponse tryEnter(EntryRequest request);

    /***/
    void forceExit(Player player, ExitReason reason);

    void onEnter(final Player player);
    void onEnter(final Player... players);

    void triggerEvent(String eventId, Player triggerer);

    void onReachCheckpoint(Player player, Checkpoint checkpoint);

    void onMobKilled(Player killer, String mobType, int amount);

    Status status();

    void shutdown();



}
