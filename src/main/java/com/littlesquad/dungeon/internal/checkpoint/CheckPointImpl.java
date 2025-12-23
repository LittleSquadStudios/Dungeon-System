package com.littlesquad.dungeon.internal.checkpoint;

import com.littlesquad.dungeon.api.checkpoint.AbstractCheckPoint;
import org.bukkit.Location;

import java.util.List;

public final class CheckPointImpl extends AbstractCheckPoint {
    public CheckPointImpl (final String id,
                           final Location loc,
                           final String respawnCheckpoint,
                           final List<String> onDeathCommands) {
        super(id, loc, respawnCheckpoint, onDeathCommands);
        CheckPointManager.register(this);
    }
}
