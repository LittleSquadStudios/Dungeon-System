package com.littlesquad.dungeon.api.status;

import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.api.session.DungeonSessionManager;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractStatus implements Status {

    public AbstractStatus() {
    }

    @Override
    public int currentPlayers() {
        return 0;
    }

    @Override //TODO: Cerca tra le session e se trovi null allora non esiste
    public boolean isPlayerInDungeon(UUID player) {

        final AtomicBoolean isIn = new AtomicBoolean(false);

        sessionManager().getSession(player).thenAccept(session -> {
            if (session != null) {
                isIn.set(true);
            }
        });

        return isIn.get();
    }

    @Override //TODO: Stessa cosa di sopra se quello vale per tutti i membri del party
    public boolean isPartyInDungeon(AbstractParty party) {

        for (final PlayerData pd : party.getOnlineMembers())
            if (isPlayerInDungeon(pd.getUniqueId()))
                return true;

        return false;
    }

    @Override
    public int playerKills(UUID player) {
        return 0;
    }

    @Override
    public int partyKills(AbstractParty party) {
        return 0;
    }

    @Override
    public int totalKills() {
        return 0;
    }

    @Override
    public int playerDeaths(UUID player) {
        return 0;
    }

    @Override
    public int totalDeaths() {
        return 0;
    }

    @Override
    public int remainingEnemies() {
        return 0;
    }

    @Override
    public List<BossRoom> bossRooms() {
        return List.of();
    }

    @Override
    public DungeonSessionManager sessionManager() {
        return null;
    }

    @Override
    public long playerTimeInDungeon(UUID uuid) {
        return 0;
    }
}
