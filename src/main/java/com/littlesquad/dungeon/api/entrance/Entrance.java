package com.littlesquad.dungeon.api.entrance;

import java.util.List;

public interface Entrance {

    int maxSlots();
    int playerMinimumLevel();
    int partyMinimumLevel();

    boolean partyRequired();

    String bypassPermission();
    String adminPermission();

    List<String> onEnterCommands();

    List<String> maxSlotsFallbackCommands ();
    List<String> partyFallbackCommands ();
    List<String> levelFallbackCommands ();
}
