package com.littlesquad.dungeon.api;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.entrance.EntryResponse;
import com.littlesquad.dungeon.internal.SessionManager;
import com.littlesquad.dungeon.internal.file.DungeonParser;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class AbstractDungeon implements Dungeon {

    /*When someone fires this command we should add him to this set
    ONLY IF HE'S WITH A PARTY, AND IF HE JOINS WE SHOULD CHECK AGAIN AND EVENTUALLY SET INTO THIS SET*/
    private final Set<UUID> leaders = ConcurrentHashMap.newKeySet();
    private DungeonParser parser;

    public AbstractDungeon(final DungeonParser parser) {
        this.parser = parser;
    }

    private static void dispatchCommands (final List<String> commands, final Player p) {
        if (p == null) {
            throw new RuntimeException("Player is offline");
        }

        commands.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .map(s -> s.startsWith("/") ? s.substring(1) : s)
                .map(s -> PlaceholderFormatter.formatPerPlayer(s, p))
                .forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
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

            return EntryResponse.SUCCESS_PARTY;
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

            return EntryResponse.SUCCESS_PARTY;
        }

        if (!hasEnoughSlots(1, leader))
            return EntryResponse.FAILURE_PER_SLOTS;

        if (leader.getLevel() < getEntrance().playerMinimumLevel())
            return EntryResponse.FAILURE_PER_LEVEL;

        return EntryResponse.SUCCESS_SOLO;
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
        player.sendMessage("test 2");
        if (isTimed()) {
            SessionManager.getInstance().startTimedSession(this,
                    player.getUniqueId(),
                    1,
                    TimeUnit.MINUTES,
                    s -> {
                final Player p = Bukkit.getPlayer(s);
                p.sendMessage("ciao");
            });

        } else
            SessionManager.getInstance()
                    .startSession(this,
                            player.getUniqueId());

        dispatchCommands(getEntrance().onEnterCommands(), player);
    }

    @Override
    public void onEnter(Player... players) {
        Arrays.stream(players).forEach(p -> dispatchCommands(getEntrance().onEnterCommands(), p));
    }

    @Override
    public void onExit(Player player) {
        SessionManager.getInstance()
                .getSession(player.getUniqueId())
                .stopSession();
    }

    @Override
    public void onExit(Player... players) {
        Arrays.stream(players)
                .map(Entity::getUniqueId)
                .toList()
                .forEach(player ->
                        SessionManager.getInstance()
                            .getSession(player)
                            .stopSession());
    }

    @Override
    public void triggerEvent(String eventId, Player triggerer) {
        Arrays.stream(getEvents())
                .filter(event -> event.getID().equals(eventId))
                .findFirst()
                .ifPresent(ev -> ev.triggerActivation(triggerer));

    }

    @Override
    public CompletableFuture<Void> triggerEventAsync(String eventId, Player triggerer) {
        return CompletableFuture.runAsync(() ->
                Arrays.stream(getEvents())
                    .filter(event -> event.getID().equals(eventId))
                    .findFirst()
                    .ifPresent(ev -> ev.triggerActivation(triggerer)));
    }

    @Override
    public void shutdown() {
        leaders.clear();
        parser = null;
    }

    private boolean isTimed() {
        return typeFlags().contains(TypeFlag.TIMED);
    }

    public DungeonParser getParser() {
        return parser;
    }
}
