package com.littlesquad.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public enum EntryResponse {
    FAILURE_PER_SLOTS,
    FAILURE_PER_PARTY,
    FAILURE_PER_LEVEL;

    private final Map<String, List<String>> fallBackCommands;

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
