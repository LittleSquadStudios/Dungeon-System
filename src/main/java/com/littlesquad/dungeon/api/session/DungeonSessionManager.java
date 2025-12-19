package com.littlesquad.dungeon.api.session;

import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.status.Status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface DungeonSessionManager {

    Status associatedStatus();

    void startTimedSession(UUID playerId,
                      long duration,
                      TimeUnit unit,
                      Consumer<UUID> onExpire);

    void startSession(UUID playerId,
                           TimeUnit unit);

    void endSession(UUID playerId, ExitReason exitReason);

    DungeonSession getSession(UUID playerId);

    List<DungeonSession> getSessions();

    void shutdown();

    void recoverActiveSessions(Consumer<UUID> onExpire);

}
