package com.littlesquad.dungeon.internal.commands;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.event.Event;
import com.littlesquad.dungeon.api.event.ObjectiveEvent;
import com.littlesquad.dungeon.api.event.TimedEvent;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.internal.SessionManager;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public final class EventCommandHandler {
    private EventCommandHandler () {}

    private static Event eventChecks (final @NotNull CommandSender sender,
                                      final @NotNull String@NotNull[] args,
                                      final AtomicReference<OfflinePlayer> p) {
        System.out.println("Cazzo 0");
        if (args.length != 3) {
            Main.getMessageProvider().sendErrorInCommand(
                    sender,
                    "event.wrong_arguments");
            return null;
        }
        final OfflinePlayer player;
        System.out.println("Cazzo 1");
        if (!(player = Bukkit
                .getOfflinePlayer(args[2]))
                .hasPlayedBefore()) {
            Main.getMessageProvider().sendErrorInCommand(
                    sender,
                    "event.inexistent_player");
            return null;
        }
        p.setPlain(player);
        final DungeonSession session;
        System.out.println("Cazzo 2");
        if ((session = SessionManager
                .getInstance()
                .getSession(player.getUniqueId()))
                == null) {
            Main.getMessageProvider().sendErrorInCommand(
                    sender,
                    "event.session_not_found");
            return null;
        }
        final Event event;
        System.out.println("Cazzo 3");
        if ((event = session
                .getDungeon()
                .getEvent(args[1]))
                == null) {
            Main.getMessageProvider().sendErrorInCommand(
                    sender,
                    "event.event_not_found");
            return null;
        }
        return event;
    }

    @SuppressWarnings("SameReturnValue")
    static boolean onTrigger (final @NotNull CommandSender sender,
                              final @NotNull String@NotNull[] args) {
        System.out.println("Cazzo 1 - 0");
        final AtomicReference<OfflinePlayer> pr = new AtomicReference<>();
        final Event event;
        if ((event = eventChecks(sender, args, pr)) == null)
            return false;
        final OfflinePlayer player;
        final AbstractParty party;
        System.out.println("Cazzo 1 - 1");
        if ((party = Main
                .getMMOCoreAPI()
                .getPlayerData(player = pr.getPlain())
                .getParty()) != null) {
            System.out.println("Cazzo 1 - 2");
            event.triggerActivation(party
                    .getOnlineMembers()
                    .parallelStream()
                    .map(SynchronizedDataHolder::getPlayer)
                    .toArray(Player[]::new));
            Main.getMessageProvider().sendMessageInCommand(
                    sender,
                    Main.getMessageProvider().getEventTriggeredForParty());
        } else {
            System.out.println("Cazzo 1 - 3");
            final Player p;
            System.out.println("Cazzo 1 - 4");
            if ((p = player.getPlayer()) == null) {
                System.out.println("Cazzo 1 - 5");
                Main.getMessageProvider().sendErrorInCommand(
                        sender,
                        "event.offline_player");
                return false;
            }
            event.triggerActivation(p);
            Main.getMessageProvider().sendMessageInCommand(
                    sender,
                    Main.getMessageProvider().getEventTriggeredForPlayer());
        }
        return false;
    }

    @SuppressWarnings("SameReturnValue")
    static boolean onDeactivate (final @NotNull CommandSender sender,
                                 final @NotNull String@NotNull[] args) {
        final AtomicReference<OfflinePlayer> pr = new AtomicReference<>();
        final Event event;
        if ((event = eventChecks(sender, args, pr)) == null)
            return false;
        final OfflinePlayer player;
        final AbstractParty party;
        final Player p;
        final Player[] players;
        if ((players = (party = Main
                .getMMOCoreAPI()
                .getPlayerData(player = pr.getPlain())
                .getParty()) != null
                ? party
                .getOnlineMembers()
                .parallelStream()
                .map(SynchronizedDataHolder::getPlayer)
                .toArray(Player[]::new)
                : (p = player
                .getPlayer()) != null
                ? new Player[]{p}
                : null)
                == null) {
            Main.getMessageProvider().sendErrorInCommand(
                    sender,
                    "event.offline_player");
            return false;
        }
        switch (event.getType()) {
            case TIMED -> ((TimedEvent) event).deActiveFor(players);
            case OBJECTIVE -> ((ObjectiveEvent) event).deActiveFor(players);
            default -> {
                Main.getMessageProvider().sendErrorInCommand(
                        sender,
                        "event.cannot_deactivate");
                return false;
            }
        }
        Main.getMessageProvider().sendMessageInCommand(
                sender,
                Main.getMessageProvider().getEventDeactivated());
        return false;
    }
}
