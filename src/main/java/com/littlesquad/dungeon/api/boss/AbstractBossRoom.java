package com.littlesquad.dungeon.api.boss;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.internal.utils.CommandUtils;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractBossRoom implements BossRoom {
    private final Dungeon dungeon;

    private final List<Player> playersIn;
    //If the player is offline, check if it's online with another player instance through the UUID!
    private final List<Player> waitingPlayers;

    public AbstractBossRoom (final Dungeon dungeon) {
        this.dungeon = dungeon;
        playersIn = new CopyOnWriteArrayList<>();
        waitingPlayers = new CopyOnWriteArrayList<>();
    }

    public Dungeon getDungeon () {
        return dungeon;
    }

    private static final Player[] emptyPlayerArray = new Player[0];
    public Player[] getPlayersIn () {
        return playersIn.toArray(emptyPlayerArray);
    }

    public boolean kick (final Runnable onSuccess, Player... players) {
        nonSwapBlock: {
            synchronized (playersIn) {
                if (playersIn.removeAll(Arrays.asList(players))) {
                    if (playersIn.isEmpty())
                        if (onePartyOnly()) try {
                            final Player p;
                            if (!waitingPlayers.isEmpty()
                                    && (p = waitingPlayers.removeFirst()) != null) {
                                final AbstractParty party;
                                final List<Player> l;
                                if ((party = Main
                                        .getMMOCoreAPI()
                                        .getPlayerData(p)
                                        .getParty()) != null) {
                                    //Ignore result...
                                    waitingPlayers.removeAll(l = party
                                            .getOnlineMembers()
                                            .stream()
                                            .map(SynchronizedDataHolder::getPlayer)
                                            .toList());
                                    if (playersIn.addAll(l)) {
                                        players = l.toArray(emptyPlayerArray);
                                        break nonSwapBlock;
                                    }
                                } else {
                                    //Always true!
                                    playersIn.add(p);
                                    players = new Player[]{p};
                                    break nonSwapBlock;
                                }
                            }
                        } catch (final NoSuchElementException _) {
                        } else {
                            final List<Player> l;
                            if (!waitingPlayers.isEmpty()
                                    && waitingPlayers
                                    .removeAll(l = waitingPlayers
                                            .stream()
                                            .limit(capacity())
                                            .toList())
                                    && playersIn
                                    .addAll(l)) {
                                players = l.toArray(emptyPlayerArray);
                                break nonSwapBlock;
                            }
                        }
                } else return false;
            }
            onSuccess.run();
            return true;
        }
        onSuccess.run();
        CommandUtils.executeMultiForMulti(
                Bukkit.getConsoleSender(),
                enqueuingCommands(),
                players);
        getBoss().spawn();
        return true;
    }

    public boolean join (final UUID playerId) {
        final Player player;
        if ((player = Bukkit.getPlayer(playerId)) == null)
            return false;
        final AbstractParty party;
        final Player[] players = (party = Main
                .getMMOCoreAPI()
                .getPlayerData(player)
                .getParty())
                != null
                ? party
                .getOnlineMembers()
                .parallelStream()
                .map(SynchronizedDataHolder::getPlayer)
                .toArray(Player[]::new)
                : new Player[]{player};
        if (players.length > capacity())
            return false;
        //Necessary!
        synchronized (playersIn) {
            if (!playersIn.isEmpty() || !playersIn.addAll(Arrays.asList(players)))
                return false;
        }
        getBoss().spawn();
        //Check boss state after calling this method!
        return true;
    }

    public void join (final Player... players) {
        List<Player> l = null;
        f: if (players.length <= capacity()) {
            synchronized (playersIn) {
                if (!playersIn.isEmpty() || !playersIn.addAll(l = Arrays.asList(players)))
                    break f;
            }
            getBoss().spawn();
            return;
        }
        final BossRoom br;
        if ((br = fallback()) != null) {
            br.join(players);
            return;
        }
        final BukkitTask task = CommandUtils.executeMultiForMulti(
                Bukkit.getConsoleSender(),
                accessDeniedCommands(),
                players);
        waitingPlayers.addAll(l != null ? l : (l = Arrays.asList(players)));
        f: /* Check again for avoiding race conditions! */ {
            synchronized (playersIn) {
                if (!playersIn.isEmpty() || !playersIn.addAll(l))
                    break f;
            }
            if (waitingPlayers.removeAll(l)) {
                task.cancel();
                if (!task.isCancelled())
                    CommandUtils.executeMultiForMulti(
                            Bukkit.getConsoleSender(),
                            enqueuingCommands(),
                            players);
                getBoss().spawn();
            }
            return;
        }
        l.parallelStream().forEach(p -> p.sendMessage(PlaceholderFormatter
                .formatPerPlayer(Main
                        .getMessageProvider()
                        .getPrefix()
                        + Main
                        .getMessageProvider()
                        .getEnqueuedForBossRoom(), p)));
    }
}
