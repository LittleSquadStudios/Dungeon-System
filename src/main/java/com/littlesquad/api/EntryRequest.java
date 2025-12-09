package com.littlesquad.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface EntryRequest {

    int maxSlots();
    int playerMinimumLevel();
    int partyMinimum();

    boolean partyRequired();

    String bypassPermission();
    String adminPermission();

    List<String> onEnterCommands();

}
