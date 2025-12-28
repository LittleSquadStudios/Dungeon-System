package com.littlesquad.dungeon.api.entrance;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class represents a fixed
 * set composed by types of failure that may happen when
 * a player tries to join a dungeon, it's intended to trigger
 * fallback commands registered in each section through the
 * configuration.
 *
 * @apiNote It's thought to make sure that an external dev can add
 * additional fallback commands if needed, dynamically, each method
 * requires to be called passing the dungeonId, and if it does not exist
 * they won't return anything. <br>
 * <b>As shown here</b> <pre>{@code
 * entrance:
 *   max_slots:
 *     limit: 100
 *     bypass_permission: "azure_dungeon.bypass_slots"
 *     admin_permission: "azure_dungeon.admin"
 *     fall_back_commands:
 *       - "/kill %player%"
 * }</pre>
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
    FAILURE_PER_LEVEL,

    FAILURE_PER_ALREADY_PROCESSING,

    FAILURE_PER_DUNGEON_BLOCKED,

    SUCCESS_SOLO,
    SUCCESS_PARTY,
    FAILURE
}
