package com.littlesquad.dungeon.api.entrance;

/**
 * This enumeration represents the reasons why
 * a player can be kicked from a dungeon.
 *
 * @since 1.0.0
 * @author LittleSquad
 * */
public enum ExitReason {

    TIME_EXPIRED, // If the time that has been assigned to a player expires when he's in the dungeon
    FINISHED, // If the player finishes the dungeon or kills the boss or bosses

    QUIT, // If the player exit the game or server
    PLUGIN_STOPPING,
    ERROR, // If an internal plugin error occurs
    KICKED // If player gets kicked by forceExit or something else like that

}
