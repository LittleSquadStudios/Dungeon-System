package com.littlesquad.dungeon.api.checkpoint;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * This class represents a checkpoint. <br>
 * A checkpoint is a sort of save-point that activates once
 * triggered. <br>
 * It defines actions on players' death and respawn to the
 * given checkpoint the player if needed. <br>
 * In fact, it is possible to create <b>technical</b> checkpoints
 * that simply manage deaths differently from the others but that
 * still respawn at another checkpoint. <br><br>
 * <b>Any checkpoint has to be triggered by an OBJECTIVE event!</b>
 * @since 1.0.0
 * @author LittleSquad
 * */
public interface Checkpoint extends Listener {

    String getID ();

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

    Checkpoint getRespawnCheckpoint ();

    /**
     *
     * */
    List<String> onDeathCommands ();

    void unlockFor (final Player... players);
}
