package com.littlesquad.dungeon.api.checkpoint;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * This class represents a checkpoint.
 * This one allows the user to define what happens when you
 * reach a checkpoint, a checkpoint can be differentiated by:
 * <ul>
 * <li>Location</li>
 * <li>Interaction</li>
 * <li>Region</li>
 * </ul>
 * Each has to be defined when gets triggered, for example
 * in the config you should put the location of the block
 * you have to interact with. <br><br>
 * <b>Any checkpoint has to be triggered by an OBJECTIVE event!</b>
 * @since 1.0.0
 * @author LittleSquad
 * */
public interface Checkpoint extends Listener {

    /**
     * @return {@link Location} the location where the checkpoint is set
     * @since 1.0.0
     * @author LittleSquad
     * */
    Location getLocation ();

    /**
     * Brings the player to this checkpoint
     * @since 1.0.0
     * @author LittleSquad
     * */
    void respawnAtCheckpoint (final Player player);

    /**
     *
     * */
    List<String> onDeathCommands ();
}
