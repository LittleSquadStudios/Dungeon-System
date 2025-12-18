package com.littlesquad.dungeon.api;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.entrance.Entrance;
import com.littlesquad.dungeon.api.entrance.EntryResponse;
import com.littlesquad.dungeon.api.session.DungeonSessionManager;
import com.littlesquad.dungeon.api.status.Status;
import com.littlesquad.dungeon.internal.file.DungeonParser;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.mmocore.api.MMOCoreAPI;
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

    /*When someone fires this command we should add him to this set
    ONLY IF HE'S WITH A PARTY, AND IF HE JOINS WE SHOULD CHECK AGAIN AND EVENTUALLY SET INTO THIS SET*/
    private final Set<UUID> leaders = ConcurrentHashMap.newKeySet();

    // Fundamental dungeon information
    private final String dungeonId;

    //TODO: In the implementations, create a constructor that accept the parameters id (String) and parser (DungeonParser)

    public AbstractDungeon(final String dungeonId) {
        this.dungeonId = dungeonId;
    }

    private static void dispatchCommands (final List<String> commands, final Player p) {
        if (p != null)
            commands.forEach(s -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderFormatter.formatPerPlayer(s, p)));
        else throw new RuntimeException("Player is offline");
    }

    @Override
    public EntryResponse tryEnter(final Player leader) {

        final PlayerData data = Main.getMMOCoreAPI().getPlayerData(leader);
        final AbstractParty party = data.getParty();

        if (getEntrance().maxSlots() == 0)
            return EntryResponse.FAILURE_PER_DUNGEON_BLOCKED;

        // First check: Are the party required? Is the player alone?

        final boolean hasParty = party != null && getOnlinePartySize(party) > 1;

        if (getEntrance().partyRequired()) {

            // Second check: Check if leader has a party if not, it will return FAILURE_PER_PARTY

            if (!hasParty) {
                dispatchCommands(getEntrance().partyFallbackCommands(), leader); // TODO: Da togliere da qui
                return EntryResponse.FAILURE_PER_PARTY;
            }

            // Third check:

            if (isPartyAlreadyProcessing(party))
                return EntryResponse.FAILURE_PER_ALREADY_PROCESSING;

            final int partySize = getOnlinePartySize(party);

            if(!hasEnoughSlots(partySize, leader)) {
                dispatchSlotFallback(party);
                return EntryResponse.FAILURE_PER_SLOTS;
            }

            if (!hasPartyMinimumLevel(party)) {
                dispatchLevelFallback(party);
                return EntryResponse.FAILURE_PER_LEVEL;
            }

            enterParty(party);
            return EntryResponse.SUCCESS;
        }

        if (hasParty) {

            if (isPartyAlreadyProcessing(party))
                return EntryResponse.FAILURE_PER_ALREADY_PROCESSING;

            final int partySize = getOnlinePartySize(party);

            if (!hasEnoughSlots(partySize, leader)) {
                dispatchSlotFallback(party);
                return EntryResponse.FAILURE_PER_SLOTS;
            }

            if (!hasPartyMinimumLevel(party)) {
                dispatchLevelFallback(party);
                return EntryResponse.FAILURE_PER_LEVEL;
            }

            enterParty(party);
            return EntryResponse.SUCCESS;

        }

        if (!hasEnoughSlots(1, leader))
            return EntryResponse.FAILURE_PER_SLOTS;

        if (leader.getLevel() < getEntrance().playerMinimumLevel())
            return EntryResponse.FAILURE_PER_LEVEL;

        enterSolo(leader);
        return EntryResponse.SUCCESS;
    }

    private boolean isPartyAlreadyProcessing(AbstractParty party) {
        for (final PlayerData pd : party.getOnlineMembers())
            if (leaders.contains(pd.getUniqueId()))
                return true;
        return false;
    }

    private int getOnlinePartySize(AbstractParty party) {
        return party.getOnlineMembers().size();
    }

    private boolean hasEnoughSlots(int incomingPlayers, Player leader) {
        final int maxSlots = getEntrance().maxSlots();

        if (maxSlots == -1)
            return true;

        if (status().currentPlayers() + incomingPlayers <= maxSlots)
            return true;

        return leader.hasPermission(getEntrance().bypassPermission())
                || leader.hasPermission(getEntrance().adminPermission());
    }

    private void dispatchSlotFallback(AbstractParty party) {
        party.getOnlineMembers()
                .stream()
                .map(SynchronizedDataHolder::getPlayer)
                .forEach(p ->
                        dispatchCommands(
                                getEntrance().maxSlotsFallbackCommands(), p));
    }

    private boolean hasPartyMinimumLevel(AbstractParty party) {
        return party.getOnlineMembers()
                .stream()
                .mapToInt(PlayerData::getLevel)
                .sum() >= getEntrance().partyMinimumLevel();
    }

    private void dispatchLevelFallback(AbstractParty party) {
        party.getOnlineMembers()
                .stream()
                .map(SynchronizedDataHolder::getPlayer)
                .forEach(p ->
                        dispatchCommands(
                                getEntrance().levelFallbackCommands(), p));
    }

    private void enterParty(AbstractParty party) {
        onEnter(party.getOnlineMembers()
                .stream()
                .map(SynchronizedDataHolder::getPlayer)
                .toArray(Player[]::new));
    }

    private void enterSolo(Player leader) {
        onEnter(leader);
    }



    @Override
    public CompletableFuture<EntryResponse> tryEnterAsync(Player p) {
        // TODO: Lascio l'onere a draky, saprei come farlo ma non voglio urla addosso ;)
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

    @Override
    public String id() {
        return dungeonId;
    }
}
