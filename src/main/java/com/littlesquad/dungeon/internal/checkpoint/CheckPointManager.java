package com.littlesquad.dungeon.internal.checkpoint;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CheckPointManager {
    private static final Checkpoint DUMMY = new Checkpoint() {
        public String getID () {
            return "";
        }
        public Location getLocation () {
            return null;
        }
        public void respawnAtCheckpoint (final Player player) {}
        public Checkpoint getRespawnCheckpoint () {
            return this;
        }
        public List<String> onDeathCommands () {
            return Collections.emptyList();
        }
        public void unlockFor (final Player... players) {}
    };
    static final Map<String, Checkpoint> checkpoints = new ConcurrentHashMap<>();

    static final Map<UUID, Checkpoint> playerCheckpoints = new ConcurrentHashMap<>();

    private CheckPointManager () {}

    public static void register (final Checkpoint checkpoint) {
        checkpoints.compute(checkpoint.getID(), (_, v) -> {
            if (v != null) {
                Main.getDungeonLogger().warning(Main
                        .getMessageProvider()
                        .getConsolePrefix()
                        + Main
                        .getMessageProvider()
                        .getMessage("config.dungeon.already_existing_checkpoint")
                        + '\''
                        + checkpoint.getID()
                        + '\'');
                return v;
            }
            return checkpoint;
        });
    }
    public static void unregister (final String id) {
        checkpoints.remove(id);
    }

    public static Checkpoint get (final String id) {
        if (id.isEmpty())
            return DUMMY;
        return checkpoints.get(id);
    }
    public static Checkpoint get (final UUID id) {
        return playerCheckpoints.get(id);
    }

    public static void setCheckPointFor (final Player player,
                                         final Checkpoint checkpoint) {
        playerCheckpoints.put(player.getUniqueId(), checkpoint);
    }

    public static void removeCheckPointFor (final Player player) {
        playerCheckpoints.remove(player.getUniqueId());
    }
    public static void removeCheckPointFor (final UUID id) {
        playerCheckpoints.remove(id);
    }

    //No need of a 'reload' method!
}
