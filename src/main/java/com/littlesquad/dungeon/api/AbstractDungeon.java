package com.littlesquad.dungeon.api;

import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.entrance.Entrance;
import com.littlesquad.dungeon.api.entrance.EntryResponse;
import com.littlesquad.dungeon.internal.file.DungeonParser;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractDungeon implements Dungeon {

    private final Set<UUID> leaders = ConcurrentHashMap.newKeySet();

    //TODO: In the implementations, create a constructor that accept the parameters id (String) and parser (DungeonParser)

    private static void dispatchCommands (final List<String> commands, final Player p) {
        commands.forEach(s -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderFormatter.formatPerPlayer(s, p)));
    }

    @Override
    public EntryResponse tryEnter(final Player leader) {

        final PlayerData data = PlayerData.get(leader.getUniqueId());
        final AbstractParty party = data.getParty();

        // First check: Are the party required? Is the player alone?

        if (getEntrance().partyRequired()) {

            if (party != null && party.countMembers() > 1) {

                for (final PlayerData pd : party.getOnlineMembers())
                    if (leaders.contains(pd.getUniqueId()))
                        return EntryResponse.FAILURE_PER_ALREADY_PROCESSING;

            } else {
                dispatchCommands(getEntrance().partyFallbackCommands(), leader);
                return EntryResponse.FAILURE_PER_PARTY;
            }

            final int currentPartyMember = party.countMembers();

            if ((status().currentPlayers() + currentPartyMember) > getEntrance().maxSlots()) {
                //TODO: Swape attento al caso in cui il 'maxSlots' è 0 (leggi il config)

                if (!leader.hasPermission(getEntrance().bypassPermission()) ||
                        !leader.hasPermission(getEntrance().adminPermission())) {
                    party.getOnlineMembers()
                            .stream()
                            .map(SynchronizedDataHolder::getPlayer)
                            .forEach(p -> dispatchCommands(
                                    getEntrance().maxSlotsFallbackCommands(),
                                    p));
                    return EntryResponse.FAILURE_PER_SLOTS;
                }

            }

            if (party.getOnlineMembers()
                    .stream()
                    .mapToInt(PlayerData::getLevel)
                    .sum()
                    < getEntrance()
                    .partyMinimumLevel()) {
                party.getOnlineMembers()
                        .stream()
                        .map(SynchronizedDataHolder::getPlayer)
                        .forEach(p -> dispatchCommands(
                                getEntrance().levelFallbackCommands(),
                                p));
                return EntryResponse.FAILURE_PER_LEVEL;
            }

            onEnter(party.getOnlineMembers()
                    .stream()
                    .map(SynchronizedDataHolder::getPlayer)
                    .toArray(Player[]::new));

            return EntryResponse.SUCCESS;
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

    public void onEnter(Player player) {
        dispatchCommands(getEntrance().onEnterCommands(), player);
    }

    @Override
    public void onEnter(Player... players) {
        Arrays.stream(players).forEach(p -> dispatchCommands(getEntrance().onEnterCommands(), p));
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

    public abstract Entrance getEntrance();
    public abstract void runTimeReload(final DungeonParser parser);

}
