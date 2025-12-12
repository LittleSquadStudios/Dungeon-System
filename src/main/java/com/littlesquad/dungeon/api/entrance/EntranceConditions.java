package com.littlesquad.dungeon.api.entrance;

import java.util.List;

public interface EntranceConditions {

    int maxSlots();
    int playerMinimumLevel();
    int partyMinimumLevel();

    boolean partyRequired();

    String bypassPermission();
    String adminPermission();

    List<String> onEnterCommands();

}
