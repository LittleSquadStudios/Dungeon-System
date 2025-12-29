package com.littlesquad.dungeon.internal.boss;

import com.littlesquad.dungeon.api.boss.BossRoom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BossRoomManager {
    private static final BossRoomManager instance = new BossRoomManager();

    private final Map<String, BossRoom> bossRooms = new ConcurrentHashMap<>();

    private BossRoomManager () {}

    public static BossRoomManager getInstance () {
        return instance;
    }

    void register (final String id, final BossRoom bossRoom) {
        bossRooms.put(id, bossRoom);
    }

    public BossRoom get (final String id) {
        return bossRooms.get(id);
    }

    public void remove (final String id) {
        bossRooms.remove(id);
    }
    public void clear () {
        bossRooms.clear();
    }
}
