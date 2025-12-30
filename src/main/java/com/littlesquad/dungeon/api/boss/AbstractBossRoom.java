package com.littlesquad.dungeon.api.boss;

import com.littlesquad.Main;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractBossRoom implements BossRoom {
    private final List<Player> playersIn;
    //If the player is offline, check if it's online with another player instance through the UUID!
    final Queue<Player> waitingPlayers;

    public AbstractBossRoom () {
        playersIn = new CopyOnWriteArrayList<>();
        waitingPlayers = new ConcurrentLinkedQueue<>();
    }

    private static final Player[] emptyPlayerArray = new Player[0];
    public Player[] getPlayersIn () {
        return playersIn.toArray(emptyPlayerArray);
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
        Arrays.stream(players)
                .forEach(p -> accessDeniedCommands()
                        .parallelStream()
                        .forEach(c -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                PlaceholderFormatter.formatPerPlayer(c, p)))));
        waitingPlayers.addAll(l != null ? l : (l = Arrays.asList(players)));
        f: /* Check again for avoiding race conditions! */ {
            synchronized (playersIn) {
                if (!playersIn.isEmpty() || !playersIn.addAll(l))
                    break f;
            }
            if (waitingPlayers.removeAll(l)) {
                Arrays.stream(players)
                        .forEach(p -> enqueuingCommands()
                                .parallelStream()
                                .forEach(c -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> Bukkit.dispatchCommand(
                                        Bukkit.getConsoleSender(),
                                        PlaceholderFormatter.formatPerPlayer(c, p)))));
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
