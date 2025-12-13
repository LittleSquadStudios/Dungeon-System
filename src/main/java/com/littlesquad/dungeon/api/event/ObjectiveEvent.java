package com.littlesquad.dungeon.api.event;

import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import org.bukkit.entity.Player;

import java.util.List;

public abstract non-sealed class ObjectiveEvent implements Event {
    @FunctionalInterface
    public interface Requirement {
        boolean check (final Player... players);
    }

    private final List<Requirement> requirements;

    protected ObjectiveEvent (final List<Requirement> requirements) {
        this.requirements = requirements;
    }

    public final boolean meetRequirements (final Player... players) {
        return requirements.parallelStream().allMatch(req -> req.check(players));
    }

    public abstract Checkpoint checkpointToUnlock ();
    public abstract BossRoom bossRoomToUnlock ();
}
