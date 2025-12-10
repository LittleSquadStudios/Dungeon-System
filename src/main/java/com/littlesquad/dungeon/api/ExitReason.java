package com.littlesquad.dungeon.api;

/**
 * This enumeration represents the reasons why
 * a player can be kicked from a dungeon.
 *
 * @since 1.0.0
 * @author LittleSquad
 * */
public enum ExitReason {

    DEATH, // Autoexplicative
    TIME_EXPIRED, // If the time that has been assigned to a player expires when he's in the dungeon
    FINISHED, // If the player finishes the dungeon or kills the boss or bosses

    QUIT, // If the player exit the game or server
    ERROR, // If an internal plugin error occurs
    KICKED // If player gets kicked by forceExit or something else like that

}
