package com.littlesquad.dungeon.api.checkpoint;

import org.bukkit.Location;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractCheckPoint implements Checkpoint {

    protected final String id;

    private volatile Location checkPointLoc;
    protected final List<String> onDeathCommands;

    public AbstractCheckPoint (final String id,
                               final Location startingLoc) {
        this.id = id;
        checkPointLoc = startingLoc;
        onDeathCommands = new CopyOnWriteArrayList<>();
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public List<String> onDeathCommands() {
        return onDeathCommands;
    }

    public void addDeathCommand(final String command) {
        onDeathCommands.add(command);
    }

    public void setLocation(final Location currentLoc) {
        checkPointLoc = currentLoc;
    }

    @Override
    public Location getLocation() {
        return checkPointLoc;
    }
}
