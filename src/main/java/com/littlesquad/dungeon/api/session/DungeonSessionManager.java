package com.littlesquad.dungeon.api.session;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface DungeonSessionManager {

    void startSession(UUID playerId,
                      long duration,
                      TimeUnit unit,
                      Consumer<UUID> onExpire);

    void endSession(UUID playerId, String exitReason);

    CompletableFuture<DungeonSession> getSession(UUID playerId);

    void shutdown();

    void recoverActiveSessions(Consumer<UUID> onExpire);

}
