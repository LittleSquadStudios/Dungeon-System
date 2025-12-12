package com.littlesquad.dungeon.api;

import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.entrance.EntranceConditions;
import com.littlesquad.dungeon.api.entrance.EntryResponse;
import com.littlesquad.dungeon.internal.file.DungeonParser;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractDungeon implements Dungeon {

    private final Set<UUID> leaders = ConcurrentHashMap.newKeySet();

    @Override
    public EntryResponse tryEnter(final Player leader) {

        final PlayerData data = PlayerData.get(leader.getUniqueId());
        final AbstractParty party = data.getParty();

        // First check: Are the party required? Is the player alone?

        if (getEntranceConditions().partyRequired()) {

            if (party != null && party.countMembers() > 1) {

                for (final PlayerData pd : party.getOnlineMembers())
                    if (leaders.contains(pd.getUniqueId()))
                        return EntryResponse.FAILURE_PER_ALREADY_PROCESSING;

            } else return EntryResponse.FAILURE_PER_PARTY;

            final int currentPartyMember = party.countMembers();

            if ((status().currentPlayers() + currentPartyMember) > getEntranceConditions().maxSlots()) {

                if (!leader.hasPermission(getEntranceConditions().bypassPermission()) ||
                        !leader.hasPermission(getEntranceConditions().adminPermission())) {
                    return EntryResponse.FAILURE_PER_SLOTS;
                }

            }

            if (party.getOnlineMembers()
                    .stream()
                    .mapToInt(PlayerData::getLevel)
                    .sum()
                    < getEntranceConditions()
                    .partyMinimumLevel())
                return EntryResponse.FAILURE_PER_LEVEL;

            onEnter(party.getOnlineMembers()
                    .stream()
                    .map(SynchronizedDataHolder::getPlayer)
                    .toArray(Player[]::new));

        } // TODO: Continuare gestione player singolo



        /*if (status().currentPlayers() > request.maxSlots()) {

        }*/




        return null;
    }

    @Override
    public CompletableFuture<EntryResponse> tryEnterAsync(Player p) {
        return null;
    }

    @Override
    public ExitReason forceExit(Player player) {
        return null;
    }

    @Override
    public void onEnter(Player player) {
        getEntranceConditions().onEnterCommands().forEach(s -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s));
        //TODO: Format the command with placeholder api
    }

    @Override
    public void onEnter(Player... players) {
        Arrays.stream(players).forEach(p ->
                getEntranceConditions().onEnterCommands().forEach(s ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s)));
        //TODO: Format the command with placeholder api
    }

    @Override
    public void onExit(Player player) {

    }

    @Override
    public void onExit(Player... players) {

    }

    @Override
    public Checkpoint getCheckPoint(String checkPointId) {
        return null;
    }

    @Override
    public void triggerEvent(String eventId, Player triggerer) {

    }

    @Override
    public CompletableFuture<Void> triggerEventAsync(String eventId, Player triggerer) {
        return null;
    }

    @Override
    public Status status() {
        return null;
    }

    @Override
    public void shutdown() {

    }

    public abstract EntranceConditions getEntranceConditions();
    public abstract void runTimeReload(final DungeonParser parser);

}
