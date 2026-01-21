package com.littlesquad.dungeon.internal.commands;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.entrance.EntryResponse;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.internal.DungeonManager;
import com.littlesquad.dungeon.internal.SessionManager;
import com.littlesquad.dungeon.internal.utils.CommandUtils;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;

public final class JoinCommandHandler {

    static boolean onLeave(final CommandSender sender) {
        if (!(sender instanceof Player p)) {
            return false;
        }

        final DungeonSession session = SessionManager
                .getInstance()
                .getSession(p.getUniqueId());

        if (session != null) {
            session.getDungeon().onExit(p);
        }

        return true;
    }

    static boolean onJoin(final CommandSender sender, final SecureRandom random, final @NotNull String... args) {
        if (!(sender instanceof Player p)) {
            return false;
        }

        final String dungeonName = args[1];

        final Dungeon dungeonToJoin = DungeonManager
                .getDungeonManager()
                .getDungeon(dungeonName)
                .orElse(DungeonManager
                        .getDungeonManager()
                        .getAllDungeons()
                        .get(random.nextInt(
                                DungeonManager.getDungeonManager().getDungeonCount()
                        )));

        if (!dungeonName.equals(dungeonToJoin.id())) {
            p.sendMessage("This dungeon is not available, however you can try this out -> " +
                    dungeonToJoin.displayName() +
                    " with this id: " +
                    dungeonToJoin.id());
            return true;
        }

        final EntryResponse entryResponse = dungeonToJoin.tryEnter(p);

        switch (entryResponse) {
            case FAILURE_PER_LEVEL -> {
                Main.getMessageProvider().sendMessageInCommand(p,
                        Main.getMessageProvider().getEntranceFailurePerLevel());
                CommandUtils.executeMulti(
                        Bukkit.getConsoleSender(),
                        dungeonToJoin
                                .getEntrance()
                                .levelFallbackCommands(),
                        p
                );
            }
            case FAILURE_PER_DUNGEON_BLOCKED -> Main.getMessageProvider().sendMessageInCommand(p, Main.getMessageProvider().getEntranceFailurePerDungeonBlocked());
            case FAILURE_PER_SLOTS -> {
                Main.getMessageProvider().sendMessageInCommand(p, Main
                        .getMessageProvider()
                        .getEntranceFailurePerSlots());
                CommandUtils.executeMulti(
                        Bukkit.getConsoleSender(),
                        dungeonToJoin
                                .getEntrance()
                                .maxSlotsFallbackCommands(),
                        p
                );
            }
            case FAILURE_PER_ALREADY_PROCESSING -> Main.getMessageProvider().sendMessageInCommand(p, Main.getMessageProvider().getEntranceFailurePerAlreadyProcessing());
            case FAILURE_PER_PARTY -> {
                Main.getMessageProvider().sendMessageInCommand(p, Main.getMessageProvider().getEntranceFailurePerParty());
                CommandUtils.executeMulti(
                        Bukkit.getConsoleSender(),
                        dungeonToJoin
                                .getEntrance()
                                .partyFallbackCommands(),
                        p
                );
            }
            case FAILURE_PER_SENDER_ALREADY_IN -> Main.getMessageProvider().sendMessageInCommand(p, Main.getMessageProvider().getEntranceFailurePerSenderAlreadyIn());
            case FAILURE_PER_MEMBER_ALREADY_IN -> Main.getMessageProvider().sendMessageInCommand(p, Main.getMessageProvider().getEntranceFailurePerMemberAlreadyIn());
            case SUCCESS_PARTY -> {

                final AbstractParty playerParty = Main.getMMOCoreAPI()
                        .getPlayerData(p)
                        .getParty();

                if (playerParty == null) {
                    Main.getMessageProvider().sendMessageInCommand(p, Main.getMessageProvider().getEntranceSuccess());
                    return false;
                }

                final Player[] partyMembers = playerParty.getOnlineMembers()
                        .stream()
                        .map(PlayerData::getPlayer)
                        .toArray(Player[]::new);

                if (partyMembers.length == 0) {
                    Main.getMessageProvider().sendMessageInCommand(p, "§cAt least one 2 members should be present in this party!");
                    return false;
                }

                dungeonToJoin.onEnter(partyMembers);
            }
            case SUCCESS_SOLO -> dungeonToJoin.onEnter(p);
        }

        return true;
    }
}
