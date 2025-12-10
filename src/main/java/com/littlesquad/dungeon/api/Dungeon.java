package com.littlesquad.dungeon.api;

import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.entrance.EntranceConditions;
import com.littlesquad.dungeon.api.entrance.EntryResponse;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
     * */
    EntryResponse tryEnter(EntranceConditions request);

    /**
     * This method equals to <code>{@link Dungeon#tryEnter(EntranceConditions)}</code> but is executed
     * in async
     * @return {@link CompletableFuture} containing {@link EntryResponse}
     * @since 1.0.0
     * @author LittleSquad
     * */
    CompletableFuture<EntryResponse> tryEnterAsync(EntranceConditions request);

    /**
     * Forces the player to exit the dungeon.
     * Regardless of the player's progress or performance, they will be
     * removed from the dungeon and an {@link ExitReason#KICKED} will be returned.
     * Additionally, the player registry will record that, at the moment this
     * method was executed, the player was forcibly removed.
     *
     * @return {@link ExitReason#KICKED}
     * @since 1.0.0
     * author LittleSquad
     */
    ExitReason forceExit(final Player player);

    /**
     * What happens every time a player joins the dungeon, is different from tryEnter
     * since there he will check various things, here you should write what happens
     * exactly after that moment.
     *
     * @since 1.0.0
     * @author LittleSquad
     * */
    void onEnter(final Player player);
    void onEnter(final Player... players);

    void onExit(final Player player);
    void onExit(final Player... players);

    Checkpoint getCheckPoint(final String checkPointId);

    void triggerEvent (final String eventId, final Player triggerer);
    CompletableFuture<Void> triggerEventAsync (final String eventId, final Player triggerer);

    Status status();

    void shutdown();
}
