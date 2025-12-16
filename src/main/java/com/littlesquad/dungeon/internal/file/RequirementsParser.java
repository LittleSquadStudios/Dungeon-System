package com.littlesquad.dungeon.internal.file;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.event.ObjectiveEvent;
import com.littlesquad.dungeon.api.event.requirement.Requirements;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RequirementsParser {
    private final FileConfiguration config;
    private final ExecutorService ex = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());

    RequirementsParser (final FileConfiguration config) {
        this.config = config;
    }

    //Cast to 'FailingRequirement' the return for checking if the parsing encountered any error...
    public Requirements parse (final ObjectiveEvent event) {
        final class VariableRequirement {
            private int current;
            private final int objective;
            private VariableRequirement(final int objective) {
                this.objective = objective;
            }
        }
        final Map<String, VariableRequirement> baseSlayRequirements = new ConcurrentHashMap<>();
        final List<Location> baseLTV = new CopyOnWriteArrayList<>();
        //noinspection ClassCanBeRecord
        final class LocationPair {
            private final Location firstPos;
            private final Location lastPos;
            private LocationPair (final Location firstPos,
                                  final Location lastPos) {
                this.firstPos = firstPos;
                this.lastPos = lastPos;
            }
        }
        final List<LocationPair> baseRTV = new CopyOnWriteArrayList<>();
        //empty key if block interaction
        final Map<Location, String> baseInteractions = new ConcurrentHashMap<>();
        final Map<String, VariableRequirement> baseItemRequirements = new ConcurrentHashMap<>();
        final class ParticipantRequirements {
            final Map<String, VariableRequirement> slayRequirements = new ConcurrentHashMap<>(baseSlayRequirements);
            //LTV = Locations To Visit
            final List<Location> nearLTV = new CopyOnWriteArrayList<>(baseLTV);
            //RTV = Regions To Visit
            final List<LocationPair> rtv = new CopyOnWriteArrayList<>(baseRTV);
            final Map<Location, String> interactions = new ConcurrentHashMap<>(baseInteractions);
            final Map<String, VariableRequirement> itemRequirements = new ConcurrentHashMap<>(baseItemRequirements);
            final AtomicBoolean completed = new AtomicBoolean();
            private ParticipantRequirements () {}
        }
        //key might be either a player or a party
        final Map<?, ParticipantRequirements> participantRequirements = new ConcurrentHashMap<>();


        //TODO: Remember to check if the event 'isActiveFor()' the players!

        config.getStringList("events."
                        + event.getID()
                        + ".requirements")
                .parallelStream()
                .forEach(requirement -> {
                    final String[] words;
                    switch ((words = requirement
                            .split(" "))
                            .length) {
                        case 3:
                            switch (words[0]) {
                                case "slay":
                                    final int mobNumber;
                                    try {
                                        mobNumber = Integer.parseInt(words[1]);
                                    } catch (final NumberFormatException _) {
                                        return failingRequirement();
                                    }
                                    final EntityType entityType;
                                    try {
                                        entityType = EntityType.valueOf(words[2]);
                                    } catch (final IllegalArgumentException _) {


                                        //TODO: Check if it's a MythicMob instead!

                                        return;
                                    }



                                    return;
                            }
                    }
                    Main.getDungeonLogger().warning(Main
                            .getMessageProvider()
                            .getPrefix()
                            + Main
                            .getMessageProvider()
                            .getMessage("config.dungeon.requirement_parsing_error")
                            + '\''
                            + requirement
                            + '\''
                            + " for "
                            + event.getDungeon().id());
                });
        return config.getString("events."
                + event.getID()
                + ".requirement_mode",
                "ALL")
                .equalsIgnoreCase("ALL")
                ? (type, e) -> ex.execute(() -> {
            switch (type) {
                case SLAY -> {
                    final ParticipantRequirements requirements;
                    if (e instanceof final EntityDeathEvent ev) {
                        if (ev.isCancelled()
                                /*|| ev.*/)
                            return;

                        final Player player;

                        //extract player...

                        final AbstractParty party;
                        requirements = participantRequirements
                                .computeIfAbsent((party = Main
                                                .getMMOCoreAPI()
                                                .getPlayerData(player)
                                                .getParty())
                                                != null
                                                ? party
                                                : player,
                                        _ -> new ParticipantRequirements());
                    } else return;



                }
            }
        }) : (type, e) -> ex.execute(() -> {

                });
    }

    public void close () {
        ex.shutdownNow();
    }
}
