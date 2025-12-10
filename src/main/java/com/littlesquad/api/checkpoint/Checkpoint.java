package com.littlesquad.api.checkpoint;

import com.littlesquad.api.Event;

import java.util.List;

/**
 *
 * This class represents a checkpoint, this one allows the user
 * to define what happens when you reach a checkpoint, a checkpoint
 * che be differentiated by
 * <ul>
 *     <li>Location</li>
 *     <li>Interaction</li>
 *     <li>Region</li>
 * </ul>
 * Each has to be defined when gets triggered, for example
 * if you chose to create a {@link AbstractInteractionCheckPoint} then in the config
 * you should put che location of the block you have to interact to
 * and the type of that block
 *
 * @see AbstractLocationCheckPoint
 * @see AbstractInteractionCheckPoint
 * @see AbstractRegionCheckPoint
 * @since 1.0.0
 * @author LittleSquad
 * */
public interface Checkpoint {

    /**
     * This will handle every custom action performed
     * every time a player or a party player reaches a
     * point, interact with something, or joins a region bound
     *
     * @apiNote This isn't directly manageable through the config
     * in this version since to keep the config straight simple and
     * fast to use we had to just add options and fallBackCommands
     * and we hadn't had the permission, time, and appropriate
     * schematic project to create a full (we just made it for conditions)
     * script config reader that could have allowed us to put
     * a set of action that you could make executed, however
     * we made this plugin able to be expanded by implementing
     * interfaces like this one and specifying what you want
     * into it
     *
     * @since 1.0.0
     * @author LittleSquad
     * */
    void onReach();


    List<Event> associatedEvents();

}
