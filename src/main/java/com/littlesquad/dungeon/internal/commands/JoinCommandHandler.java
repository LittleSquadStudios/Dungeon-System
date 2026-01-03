package com.littlesquad.dungeon.internal.commands;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
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
import java.util.Arrays;

public final class JoinCommandHandler {

    static boolean onLeave(final CommandSender sender) {
        System.out.println("=== DEBUG onLeave START ===");
        System.out.println("Sender: " + sender.getName());
        System.out.println("Is Player: " + (sender instanceof Player));

        if (!(sender instanceof Player p)) {
            System.out.println("Sender is not a player -> return false");
            return false;
        }

        System.out.println("Player UUID: " + p.getUniqueId());

        final DungeonSession session = SessionManager
                .getInstance()
                .getSession(p.getUniqueId());

        System.out.println("Player has active session: " + (session != null));

        if (session != null) {
            System.out.println("Session found, dungeon: " + session.getDungeon().id());
            System.out.println("Calling onExit for player");

            session.getDungeon()
                    .onExit(p);

            System.out.println("Ending session with reason: QUIT");
            SessionManager
                    .getInstance()
                    .endSession(p.getUniqueId(),
                            ExitReason.QUIT);

            System.out.println("Session ended successfully");
        } else {
            System.out.println("No active session found for player");
        }

        System.out.println("=== DEBUG onLeave END ===");
        return true;
    }

    static boolean onJoin(final CommandSender sender, final SecureRandom random, final @NotNull String...args) {
        System.out.println("=== DEBUG onJoin START ===");
        System.out.println("Sender: " + sender.getName());
        System.out.println("Args: " + Arrays.toString(args));
        System.out.println("Is Player: " + (sender instanceof Player));

        if (!(sender instanceof Player p)) {
            System.out.println("Sender is not a player -> return false");
            return false;
        }

        System.out.println("Player UUID: " + p.getUniqueId());

        final String dungeonName = args[1];
        System.out.println("Requested dungeon name: " + dungeonName);

        System.out.println("Fetching dungeon from DungeonManager...");
        final Dungeon dungeonToJoin = DungeonManager
                .getDungeonManager()
                .getDungeon(dungeonName)
                .orElse(DungeonManager
                        .getDungeonManager()
                        .getAllDungeons()
                        .get(random.nextInt(0,
                                DungeonManager
                                        .getDungeonManager()
                                        .getDungeonCount())
                        ));

        System.out.println("Dungeon to join: " + dungeonToJoin.id());
        System.out.println("Dungeon display name: " + dungeonToJoin.displayName());

        if (!dungeonName.equals(dungeonToJoin.id())) {
            System.out.println("Requested dungeon doesn't exist, suggesting alternative");

            p.sendMessage("This dungeon is not available, however you can try this out -> " +
                    dungeonToJoin.displayName() +
                    " with this id: " +
                    dungeonToJoin.id());

        } else {
            System.out.println("Dungeon found, calling tryEnter...");
            final var entryResponse = dungeonToJoin.tryEnter(p);
            System.out.println("tryEnter returned: " + entryResponse);

            switch (entryResponse) {
                case FAILURE_PER_LEVEL -> {
                    System.out.println("Handler: FAILURE_PER_LEVEL");
                    p.sendMessage("Your level or your party level isn't enough");

                    System.out.println("Level fallback commands: " +
                            dungeonToJoin.getEntrance().levelFallbackCommands());
                    CommandUtils.executeMulti(
                            Bukkit.getConsoleSender(),
                            dungeonToJoin.getEntrance().levelFallbackCommands(),
                            p);
                }
                case FAILURE_PER_DUNGEON_BLOCKED -> {
                    System.out.println("Handler: FAILURE_PER_DUNGEON_BLOCKED");
                    p.sendMessage("Dungeon is blocked");
                }
                case FAILURE_PER_SLOTS -> {
                    System.out.println("Handler: FAILURE_PER_SLOTS");
                    p.sendMessage("There's no space left");

                    System.out.println("Max slots fallback commands: " +
                            dungeonToJoin.getEntrance().maxSlotsFallbackCommands());
                    CommandUtils.executeMulti(
                            Bukkit.getConsoleSender(),
                            dungeonToJoin.getEntrance().maxSlotsFallbackCommands(),
                            p);
                }
                case FAILURE_PER_ALREADY_PROCESSING -> {
                    System.out.println("Handler: FAILURE_PER_ALREADY_PROCESSING");
                    p.sendMessage("One of your teammate has already started dungeon join process");
                }
                case FAILURE_PER_PARTY -> {
                    System.out.println("Handler: FAILURE_PER_PARTY");
                    p.sendMessage("Your party sucks");

                    System.out.println("Party fallback commands: " +
                            dungeonToJoin.getEntrance().partyFallbackCommands());
                    CommandUtils.executeMulti(
                            Bukkit.getConsoleSender(),
                            dungeonToJoin.getEntrance().partyFallbackCommands(),
                            p);
                }
                case FAILURE_PER_SENDER_ALREADY_IN -> {
                    System.out.println("Handler: FAILURE_PER_SENDER_ALREADY_IN");
                    p.sendMessage("You're already in a dungeon");
                }
                case FAILURE_PER_MEMBER_ALREADY_IN -> {
                    System.out.println("Handler: FAILURE_PER_MEMBER_ALREADY_IN");
                    p.sendMessage("There's already a party member in");
                }
                case SUCCESS_PARTY -> {
                    System.out.println("Handler: SUCCESS_PARTY - Processing party entry");

                    final AbstractParty playerParty = Main.getMMOCoreAPI()
                            .getPlayerData(p)
                            .getParty();

                    System.out.println("Player party object: " + (playerParty != null ? "EXISTS" : "NULL"));

                    if (playerParty == null) {
                        System.out.println("ERROR: SUCCESS_PARTY but party is null!");
                        p.sendMessage("You should be in a party in order to join this dungeon!");
                        return false;
                    }

                    final Player[] partyMembers = playerParty.getOnlineMembers()
                            .stream()
                            .map(PlayerData::getPlayer)
                            .toArray(Player[]::new);

                    System.out.println("Party members array: " + Arrays.toString(partyMembers));
                    System.out.println("Party members count: " + partyMembers.length);

                    if (partyMembers.length == 0) {
                        System.out.println("ERROR: No online party members!");
                        p.sendMessage("No party member is online!");
                        return false;
                    }

                    System.out.println("Calling onEnter with party members...");
                    dungeonToJoin.onEnter(partyMembers);
                    System.out.println("onEnter(partyMembers) completed");
                }
                case SUCCESS_SOLO -> {
                    System.out.println("Handler: SUCCESS_SOLO - Processing solo entry");
                    System.out.println("Calling onEnter with single player...");
                    dungeonToJoin.onEnter(p);
                    System.out.println("onEnter(player) completed");
                }
            }
        }

        System.out.println("=== DEBUG onJoin END ===");
        return true;
    }

}