package com.littlesquad.dungeon.api.checkpoint;

import org.bukkit.Location;

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
public interface Checkpoint {
    Location getLocation ();

    Checkpoint respawnAtCheckpoint ();

    List<String> onDeathCommands ();
}
