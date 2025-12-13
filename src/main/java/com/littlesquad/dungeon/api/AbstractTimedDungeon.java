package com.littlesquad.dungeon.api;

import com.littlesquad.dungeon.api.entrance.Entrance;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.internal.file.DungeonParser;
import org.bukkit.entity.Player;

import java.util.Set;

public abstract class AbstractTimedDungeon extends AbstractDungeon implements TimedDungeon {

    public AbstractTimedDungeon(String dungeonId, DungeonParser parser) {
        super(dungeonId, parser);
    }

    @Override
    public Entrance getEntrance() {
        return null;
    }

    @Override
    public void runTimeReload(DungeonParser parser) {

    }

    @Override
    public String displayName() {
        return "";
    }

    @Override
    public Set<TypeFlag> typeFlags() {
        return Set.of();
    }

    @Override
    public ExitReason forceExit(Player player) {
        return null;
    }
}
