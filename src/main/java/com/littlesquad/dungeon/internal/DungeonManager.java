package com.littlesquad.dungeon.internal;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.internal.file.FileManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class DungeonManager {

    private static final Logger LOGGER = Logger.getLogger(DungeonManager.class.getName());
    private static final DungeonManager dm = new DungeonManager();

    private final ConcurrentHashMap<String, Dungeon> dungeons;
    private volatile boolean initialized = false;

    private DungeonManager() {
        this.dungeons = new ConcurrentHashMap<>();
    }

    public static DungeonManager getDungeonManager() {
        return dm;
    }

    public void initDungeons() {
        if (initialized) {
            LOGGER.warning("Dungeon already initied");
            return;
        }

        LOGGER.info("Starting loading ...");

        FileManager.getDungeons().forEach((dungeonId, parser) -> {
            try {
                final Dungeon dungeon = new DungeonImpl(parser);
                dungeons.put(dungeonId, dungeon);
                LOGGER.info("Dungeon loaded: " + dungeonId);
            } catch (Exception e) {
                LOGGER.severe("Error during dungeon load " + dungeonId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

        initialized = true;
        LOGGER.info(dungeons.size() + " dungeons loaded");

    }

    public Optional<Dungeon> getDungeon(String dungeonId) {
        if (!initialized) {
            LOGGER.warning("!");
        }
        return Optional.ofNullable(dungeons.get(dungeonId));
    }

    public List<Dungeon> getAllDungeons() {
        return dungeons
                .values()
                .stream()
                .toList();
    }

    public boolean hasDungeon(String dungeonId) {
        return dungeons.containsKey(dungeonId);
    }

    public boolean registerDungeon(String dungeonId, Dungeon dungeon) {
       return dungeons.putIfAbsent(dungeonId, dungeon) == null;
    }

    public boolean unregisterDungeon(String dungeonId) {
        return dungeons.remove(dungeonId) != null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getDungeonCount() {
        return dungeons.size();
    }

    public void clear() {

        //TODO: End here all the session and do cleanup like unregistering dungeon checkpoints ecc...

        dungeons.clear();
        initialized = false;
    }
}
