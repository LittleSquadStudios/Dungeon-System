package com.littlesquad.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * {@link EntryResponse This class} represents a fixed
 * set composed by types of failure that may happen when
 * a player tries to join a dungeon, it's though to lunch
 * fallback commands registered in each section through the
 * configuration
 *
 * @apiNote It's though to make sure that an external dev can add
 * additional fallback commands if needed, dynamically, each method
 * requires to be called passing the dungeonId, and if it does not exist
 * they won't return anything <b>As shown here</b> <pre>{@code
 * entrance:
 *   max_slots:
 *     limit: 100
 *     bypass_permission: "azure_dungeon.bypass_slots"
 *     admin_permission: "azure_dungeon.admin"
 *     fall_back_commands:
 *       - "/kill %player%"
 * }</pre>
 *
 * If you call {@link EntryResponse#getFallBackCommands} will return this in the case you
 * chose {@link EntryResponse#FAILURE_PER_SLOTS}<br><br>
 *
 * <h1>Make sure <b>dungeonId</b> isn't null</h1>
 *
 * @author LittleSquad
 * @since 1.0.0
 * */
public enum EntryResponse {

    /**
     * Represents the config section <pre>{@code
     *   max_slots:
     *     fall_back_commands:
     *       - "/kill %player%"
     * }</pre>
     * @author LittleSquad
     * @since 1.0.0
     * */
    FAILURE_PER_SLOTS,

    /**
     * Represents the config section <pre>{@code
     *   party:
     *     fall_back_commands:
     *       - "/kill %player%"
     * }</pre>
     * @author LittleSquad
     * @since 1.0.0
     * */
    FAILURE_PER_PARTY,

    /**
     * Represents the config section <pre>{@code
     *   level:
     *     fall_back_commands:
     *       - "/kill %player%"
     * }</pre>
     * @author LittleSquad
     * @since 1.0.0
     * */
    FAILURE_PER_LEVEL;

    private final Map<String, List<String>> fallBackCommands; // Contains all dungeon and their relative commands

    EntryResponse() {
        this.fallBackCommands = new ConcurrentHashMap<>();
    }

    public void addCommand(final String dungeonId, final String command) {
        fallBackCommands.computeIfAbsent(dungeonId, k -> new CopyOnWriteArrayList<>()).add(command);
    }

    public void addCommands(final String dungeonId, final List<String> commands) {
        fallBackCommands.put(dungeonId, new CopyOnWriteArrayList<>(commands));
    }

    public void clearCommands() {
        fallBackCommands.clear();
    }

    public List<String> getFallBackCommands(final String dungeonId) {
        return fallBackCommands.getOrDefault(dungeonId, Collections.emptyList());
    }
}
