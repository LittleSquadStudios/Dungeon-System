package com.littlesquad.dungeon.api.checkpoint;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractCheckPoint implements Checkpoint {

    private Location checkPointLoc;
    private final List<String> fallBackCommands;

    public AbstractCheckPoint(final Location startingLoc) {
        checkPointLoc = startingLoc;
        fallBackCommands = new CopyOnWriteArrayList<>();
    }

    @Override
    public List<String> onDeathCommands() {
        return fallBackCommands;
    }

    public void addDeathCommand(final String command) {
        fallBackCommands.add(command);
    }

    public void setLocation(final Location currentLoc) {
        checkPointLoc = currentLoc;
    }

    @Override
    public Location getLocation() {
        return checkPointLoc;
    }

    @EventHandler
    public void playerDeath(final PlayerDeathEvent e) {

    }

}
