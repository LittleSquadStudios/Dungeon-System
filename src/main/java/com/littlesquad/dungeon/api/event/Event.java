package com.littlesquad.dungeon.api.event;

import com.littlesquad.dungeon.api.Dungeon;
import org.bukkit.event.Listener;

import java.util.List;

public sealed interface Event extends Listener permits ObjectiveEvent {

    Dungeon getDungeon ();
    String getID ();

    EventType getType ();

    //Remember that events are not active since creation!
    void triggerActivation ();
    boolean isActive ();

    //Remember that events deactivate right before executing commands!
    List<String> commands ();
}
