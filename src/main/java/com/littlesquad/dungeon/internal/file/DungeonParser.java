package com.littlesquad.dungeon.internal.file;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.entrance.Entrance;
import com.littlesquad.dungeon.api.event.Event;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ClassCanBeRecord")
public final class DungeonParser {
    private final FileConfiguration config;

    DungeonParser (final FileConfiguration config) {
        this.config = config;
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

    public RequirementsParser getRequirementParser () {
        return new RequirementsParser(config);
    }

    public Event[] getEvents (final Dungeon d) {
        final ConfigurationSection cs;
        if ((cs = config.getConfigurationSection("events")) != null) {
            return cs.getKeys(false)
                    .parallelStream()
                    .map(key -> {



                        return null;
                    })
                    .toArray(Event[]::new);
        } else return new Event[0];
    }
}
