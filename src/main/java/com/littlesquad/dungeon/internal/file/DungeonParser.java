package com.littlesquad.dungeon.internal.file;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.entrance.Entrance;
import com.littlesquad.dungeon.api.event.Event;
import com.littlesquad.dungeon.api.event.EventType;
import com.littlesquad.dungeon.api.event.structural.EnvironmentEvent;
import com.littlesquad.dungeon.api.rewards.Reward;
import com.littlesquad.dungeon.internal.boss.BossRoomImpl;
import com.littlesquad.dungeon.internal.checkpoint.CheckPointImpl;
import com.littlesquad.dungeon.internal.event.ObjectiveEventImpl;
import com.littlesquad.dungeon.internal.event.StructuralEventImpl;
import com.littlesquad.dungeon.internal.event.TimedEventImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class DungeonParser {
    private final String id;
    private final FileConfiguration config;
    private final RequirementsParser requirementsParser;
    private final RewardParser rewardParser;

    DungeonParser (final String id,
                   final FileConfiguration config) {
        this.id = id;
        this.config = config;
        rewardParser = new RewardParser(config);
        requirementsParser = new RequirementsParser(config);
    }

    public String getId () {
        return id;
    }

    public String displayName () {
        return config
                .getString("display_name", "")
                .replaceAll("&", "§");
    }

    public World getWorld () {
        final String worldName;
        return (worldName = config.getString("world")) != null
                ? Bukkit.getWorld(worldName)
                : null;
    }

    public boolean isPvP () {
        return config.getBoolean("pvp", false);
    }

    public boolean isTimeLimited () {
        return config.getBoolean("time.limited", false);
    }
    public int getTimeAmount () {
        return config.getInt("time.amount");
    }
    public TimeUnit getTimeUnit () {
        try {
            return TimeUnit.valueOf(config.getString("time.unit"));
        } catch (final Throwable _) {
            return null;
        }
    }

    public List<Reward> getRewards() {
        System.out.println(id);
        return rewardParser.parse();
    }

    public Entrance getEntrance () {
        final int maxSlots = config.getInt("entrance.max_slots.limit", -1);
        final String bypassPermission = config.getString("entrance.max_slots.bypass_permission", "");
        final String adminPermission = config.getString("entrance.max_slots.admin_permission", "");
        final List<String> maxSlotsFallbackCommands = config.getStringList("entrance.max_slots.fall_back_commands");
        final boolean partyRequired = config.getBoolean("entrance.party.required", false);
        final List<String> partyFallbackCommands = config.getStringList("entrance.party.fall_back_commands");
        final int playerMinimumLevel = config.getInt("entrance.level.personal_minimum", 0);
        final int partyMinimumLevel = config.getInt("entrance.level.party_minimum", 0);
        final List<String> levelFallbackCommands = config.getStringList("entrance.level.fall_back_commands");
        final List<String> onEnterCommands = config.getStringList("entrance.successful_entrance_commands");
        return new Entrance() {
            public int maxSlots () {
                return maxSlots;
            }

            public int playerMinimumLevel () {
                return playerMinimumLevel;
            }
            public int partyMinimumLevel () {
                return partyMinimumLevel;
            }

            public boolean partyRequired () {
                return partyRequired;
            }

            public String bypassPermission () {
                return bypassPermission;
            }
            public String adminPermission () {
                return adminPermission;
            }

            public List<String> onEnterCommands () {
                return onEnterCommands;
            }

            public List<String> maxSlotsFallbackCommands () {
                return maxSlotsFallbackCommands;
            }
            public List<String> partyFallbackCommands () {
                return partyFallbackCommands;
            }
            public List<String> levelFallbackCommands () {
                return levelFallbackCommands;
            }
        };
    }

    public Event[] getEvents (final Dungeon d) {
        final ConfigurationSection cs;
        if ((cs = config.getConfigurationSection("events")) != null) {
            return cs.getKeys(false)
                    .parallelStream()
                    .map(key -> {
                        try {
                            final EventType type = EventType.valueOf(config.getString("events." + key + ".type"));
                            return switch (type) {
                                case TIMED -> {
                                    try {
                                        final TimeUnit unit = TimeUnit.valueOf(config.getString("events." + key + ".time.unit"));
                                        yield new TimedEventImpl(
                                                d,
                                                key,
                                                config.getStringList("events." + key + ".commands"),
                                                config.getBoolean("events." + key + ".is_fixed", true),
                                                config.getLong("events." + key + ".time.amount", 0L),
                                                unit);
                                    } catch (final Throwable _) {
                                        yield null;
                                    }
                                }
                                case OBJECTIVE -> new ObjectiveEventImpl(
                                        d,
                                        key,
                                        requirementsParser,
                                        config.getStringList("events." + key + ".commands"),
                                        config.getString("events." + key + ".checkpoint", ""),
                                        config.getString("events." + key + ".boss-room", ""));
                                case STRUCTURAL -> {
                                    try {
                                        final EnvironmentEvent environmentEvent = EnvironmentEvent.valueOf(config.getString("events." + key + ".environment_event"));
                                        final List<String> materialStrings = config.getStringList("events." + key + ".block_types");
                                        final Material[] mArray = new Material[materialStrings.size()];
                                        for (int i = 0; i < mArray.length; ++i)
                                            mArray[i] = Material.valueOf(materialStrings.get(i));
                                        final String loc = config.getString("events." + key + ".area", "0 0 0 0 0 0");
                                        int i;
                                        yield new StructuralEventImpl(
                                                d,
                                                key,
                                                config.getStringList("events." + key + ".commands"),
                                                environmentEvent,
                                                mArray,
                                                new Location[]{
                                                        new Location(
                                                                d.getWorld(),
                                                                Double.parseDouble(loc.substring(0, i = loc.indexOf(' ', 1))),
                                                                Double.parseDouble(loc.substring(i + 1, i = loc.indexOf(' ', i + 2))),
                                                                Double.parseDouble(loc.substring(i + 1, i = loc.indexOf(' ', i + 2)))),
                                                        new Location(
                                                                d.getWorld(),
                                                                Double.parseDouble(loc.substring(i + 1, i = loc.indexOf(' ', i + 2))),
                                                                Double.parseDouble(loc.substring(i + 1, i = loc.indexOf(' ', i + 2))),
                                                                Double.parseDouble(loc.substring(i + 1)))
                                                },
                                                config.getStringList("events." + key + ".conditioned_by")
                                                        .stream()
                                                        .parallel()
                                                        .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet)));
                                    } catch (final Throwable _) {
                                        yield null;
                                    }
                                }
                            };
                        } catch (final Throwable _) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toArray(Event[]::new);
        } else return new Event[0];
    }

    public Checkpoint[] getCheckpoints (final Dungeon d) {
        final ConfigurationSection cs;
        if ((cs = config.getConfigurationSection("checkpoints")) != null) {
            return cs.getKeys(false)
                    .parallelStream()
                    .map(key -> {
                        try {
                            final String loc = config.getString("checkpoints." + key + ".location", "0 0 0");
                            int i;
                            return new CheckPointImpl(
                                    key,
                                    new Location(
                                            d.getWorld(),
                                            Double.parseDouble(loc.substring(0, i = loc.indexOf(' ', 1))),
                                            Double.parseDouble(loc.substring(i + 1, i = loc.indexOf(' ', i + 2))),
                                            Double.parseDouble(loc.substring(i + 1))),
                                    config.getString("checkpoints." + key + ".respawn_at_checkpoint", key),
                                    config.getStringList("checkpoints." + key + ".on_death_commands"));
                        } catch (final Throwable _) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toArray(Checkpoint[]::new);
        } else return new Checkpoint[0];
    }

    public BossRoom[] getBossRooms (final Dungeon d) {
        final ConfigurationSection cs;
        if ((cs = config.getConfigurationSection("boss_rooms")) != null) {
            return cs.getKeys(false)
                    .parallelStream()
                    .map(key -> {
                        try {
                            final TimeUnit unit0 = TimeUnit.valueOf(config.getString("boss_rooms." + key + ".time.max_boss_fight_duration.unit"));
                            final TimeUnit unit1 = TimeUnit.valueOf(config.getString("boss_rooms." + key + ".time.kick_after_completion.unit"));
                            final String loc = config.getString("boss_rooms." + key + ".boss.location", "0 0 0");
                            int i;
                            final List<Reward> allRewards = d.rewards();
                            return new BossRoomImpl(
                                    d,
                                    key,
                                    config.getInt("boss_rooms." + key + ".max_players_in", 0),
                                    config.getBoolean("boss_rooms." + key + ".max_one_party_at_a_time", false),
                                    config.getStringList("boss_rooms." + key + ".access_denied_commands"),
                                    config.getString("boss_rooms." + key + ".fallback_boss_room", ""),
                                    config.getStringList("boss_rooms." + key + ".enqueuing_commands"),
                                    config.getLong("boss_rooms." + key + ".time.max_boss_fight_duration.amount", 0L),
                                    unit0,
                                    config.getStringList("boss_rooms." + key + ".time.max_boss_fight_duration.time_out_commands"),
                                    config.getLong("boss_rooms." + key + ".time.kick_after_completion.amount", 0L),
                                    unit1,
                                    config.getInt("boss_rooms." + key + ".boss.party_level"),
                                    config.getInt("boss_rooms." + key + ".boss.multiplier"),
                                    config.getInt("boss_rooms." + key + ".boss.exponent"),
                                    config.getInt("boss_rooms." + key + ".boss.max_level"),
                                    config.getInt("boss_rooms." + key + ".boss.base_level"),
                                    config.getString("boss_rooms." + key + ".boss.name", ""),
                                    new Location(d.getWorld(),
                                            Double.parseDouble(loc.substring(0, i = loc.indexOf(' ', 1))),
                                            Double.parseDouble(loc.substring(i + 1, i = loc.indexOf(' ', i + 2))),
                                            Double.parseDouble(loc.substring(i + 1))),
                                    config.getStringList("boss_rooms." + key + ".rewards")
                                            .parallelStream()
                                            .map(id -> allRewards
                                                    .parallelStream()
                                                    .filter(r -> r.id().equals(id))
                                                    .findAny()
                                                    .orElse(null))
                                            .filter(Objects::nonNull)
                                            .toList());
                        } catch (final Throwable _) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toArray(BossRoom[]::new);
        } else return new BossRoom[0];
    }
}
