package com.littlesquad.dungeon.internal.boss;

import com.littlesquad.dungeon.api.boss.Boss;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.rewards.Reward;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossRoomManager {
    private static final BossRoomManager instance = new BossRoomManager();

    private static final BossRoom DUMMY = new BossRoom() {
        public String getID () {
            return "";
        }
        public boolean join (final UUID playerId) {
            return false;
        }
        public void join (final Player... players) {}
        public int capacity () {
            return 0;
        }
        public boolean onePartyOnly () {
            return false;
        }
        public List<String> accessDeniedCommands () {
            return Collections.emptyList();
        }
        public BossRoom fallback () {
            return null;
        }
        public List<String> enqueuingCommands () {
            return Collections.emptyList();
        }
        public Boss getBoss () {
            return null;
        }
        public List<Reward> rewards () {
            return Collections.emptyList();
        }
        public Player[] getPlayersIn () {
            return new Player[0];
        }
    };

    private final Map<String, BossRoom> bossRooms = new ConcurrentHashMap<>();

    private BossRoomManager () {}

    public static BossRoomManager getInstance () {
        return instance;
    }

    void register (final String id, final BossRoom bossRoom) {
        bossRooms.put(id, bossRoom);
    }

    public BossRoom get (final String id) {
        if (id.isEmpty())
            return DUMMY;
        return bossRooms.get(id);
    }

    public void remove (final String id) {
        bossRooms.remove(id);
    }
    public void clear () {
        bossRooms.clear();
    }
}
