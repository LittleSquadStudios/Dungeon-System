package com.littlesquad.dungeon.api.status;

import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.session.DungeonSessionManager;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;

/**
 * This class represents the status of the dungeon
 * and keep track of this information:
 * <ul>
 *     <li>Players in the dungeon</li>
 *     <li>Player kills</li>
 *     <li>Party kills</li>
 *     <li>Death for each player</li>
 *     <li>Entities in the dungeon</li>
 *     <li>Active bossrooms</li>
 * </ul>
 * This class is also a {@link Listener} so it waits for events to be fired
 * and will handle each call, saving and handling all data and eventually push
 * them on MySQL
 *
 * @since 1.0.0
 * @author LittleSquad
 * */
public interface Status extends Listener {

    /**
     * @return {@link Boolean} if pvp is allowed in this dungeon
     * @since 1.0.0
     * @author LittleSquad
     * */
    boolean isPvp();

    /**
     * @return {@link Integer} showing how many players are active in the dungeon
     * @since 1.0.0
     * @author LittleSquad
     * */
    int currentPlayers();

    /**
     * @param player UUID of the player you're looking for
     * @return {@link Boolean} if the player is in the dungeon
     * @since 1.0.0
     * @author LittleSquad
     * */
    boolean isPlayerInDungeon(UUID player);
    boolean isPartyInDungeon(AbstractParty party);

    int playerKills(UUID player);
    int partyKills(AbstractParty party);
    int totalKills();

    int playerDeaths(UUID player);
    int totalDeaths();

    int remainingEnemies();

    /**
     * It returns a list of fixed bossrooms defined in the config section <code>boss-rooms</code>
     * @return {@link List} of {@link BossRoom BossRooms}
     * @since 1.0.0
     * @author LittleSquad
     * */
    List<BossRoom> bossRooms();

    /**
     *
     *
     * */
    DungeonSessionManager sessionManager();

    /**
     * Calculates how long the player has stayed in the dungeon
     * since the first join event, which is fired when the player executes the command:
     * <br><code>/dungeon join 'dungeon_id'</code>.
     * The tracking starts at this point. If the dungeon is timed and the player
     * stays longer than the allowed time, the recorded time will be capped
     * to the maximum configured duration.
     * Possible return values:
     * <ul>
     *     <li>-1 -> You know that the player is not in the dungeon</li>
     *     <li>0 -> Time tracking isn't working or is disabled</li>
     * </ul>
     *
     * @return {@link Long} seconds
     * @since 1.0.0
     * @author LittleSquad
     * */
    long playerTimeInDungeon(final UUID uuid);


}
