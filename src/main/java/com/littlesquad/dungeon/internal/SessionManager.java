package com.littlesquad.dungeon.internal;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.session.DungeonSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class SessionManager {
    private static final SessionManager manager = new SessionManager();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return manager;
    }

    private final ConcurrentHashMap<UUID, DungeonSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> timedTasks = new ConcurrentHashMap<>();
    private final Map<Dungeon, List<DungeonSession>> dungeonSessions = new ConcurrentHashMap<>();

    public void startSession(final Dungeon dungeon, UUID playerId) {
        final DungeonSession session = createSessionInstance(dungeon, playerId);
        sessions.put(playerId, session);
        dungeonSessions.computeIfAbsent(dungeon, _ -> new CopyOnWriteArrayList<>())
                .add(session);
    }

    public void startTimedSession(final Dungeon dungeon,
                                  UUID playerId,
                                  long duration,
                                  TimeUnit unit,
                                  Consumer<UUID> onExpire) {

        final DungeonSession session = createSessionInstance(dungeon, playerId);
        sessions.put(playerId, session);
        dungeonSessions.computeIfAbsent(dungeon, _ -> new CopyOnWriteArrayList<>())
                .add(session);

        timedTasks.put(playerId,
                Main.getScheduledExecutor().schedule(() -> {
                    if (session.isActive()) {
                        onExpire.accept(playerId);
                        endSession(playerId, ExitReason.TIME_EXPIRED);
                    }
                }, duration, unit));

    }

    public void endSession(UUID playerId, ExitReason exitReason) {

        final DungeonSession session = sessions.remove(playerId);

        if (session == null)
            return;

        final ScheduledFuture<?> task = timedTasks.remove(playerId);
        if (task != null && !task.isDone()) {
            if(!task.cancel(true)){
                System.err.println("Failed to cancel timed task for player: " + playerId);
            }
        }

        final Dungeon dungeon = session.getDungeon();
        final List<DungeonSession> dungeonSessionSet = dungeonSessions.get(dungeon);

        if (dungeonSessionSet != null) {
            if (dungeonSessionSet.remove(session)) {
                session.stopSession(exitReason);
                System.out.println("Stopped session for: " + playerId);
            } else {
                System.err.println("Warning: Session not found in dungeon list for player: " + playerId);
                session.stopSession(exitReason);
            }
        } else {
            System.err.println("Warning: No session list found for dungeon: " + dungeon.id());
            session.stopSession(exitReason);
        }

    }

    public DungeonSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public List<DungeonSession> getSessions() {
        return List.copyOf(sessions.values());
    }

    public List<DungeonSession> getDungeonSessions (final Dungeon dungeon) {
        return dungeonSessions.getOrDefault(dungeon, Collections.emptyList());
    }

    public void shutdown() {
        dungeonSessions.clear();
        sessions.values().forEach(session -> endSession(session.playerId(), ExitReason.PLUGIN_STOPPING));
        sessions.clear();
    }

    public void onReload() {

    }

    public void recoverActiveSessions(final UUID playerId, final Consumer<UUID> onExpire) {

        final Player p = Bukkit.getPlayer(playerId);

        if (p == null)
            return;

        String sql = """
                    SELECT 
                        pr.pr_id,
                        pr.dungeon_id,
                        pr.player_id,
                        pr.deaths,
                        pr.enter_time,
                        pr.total_kills,
                        pr.damage_dealt,
                        pr.damage_taken,
                        d.dungeon_name,
                        tr.max_complete_time,
                        tr.unit_type
                    FROM player_runs pr
                    INNER JOIN dungeon d ON pr.dungeon_id = d.dungeon_id
                    LEFT JOIN time_restrictions tr ON d.dungeon_id = tr.dungeon_id
                    WHERE pr.player_id = (SELECT player_id FROM player WHERE uuid = ?)
                      AND pr.exit_reason IN ('PLUGIN_STOPPING', 'ERROR', 'KICKED')
                      AND pr.exit_time IS NULL
                    ORDER BY pr.enter_time DESC
                    LIMIT 1
                    """;

        Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            try (conn;
                 final PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());

                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        final int runId = rs.getInt("pr_id");
                        final String dungeonName = rs.getString("dungeon_name");
                        final int deaths = rs.getInt("deaths");
                        final Timestamp enterTime = rs.getTimestamp("enter_time");
                        final int totalKills = rs.getInt("total_kills");
                        final double damageDealt = rs.getDouble("damage_dealt");
                        final double damageTaken = rs.getDouble("damage_taken");

                        final Long maxCompleteTime = rs.getObject("max_complete_time", Long.class);
                        final String unitType = rs.getString("unit_type");

                        final Dungeon dungeon = DungeonManager
                                .getDungeonManager()
                                .getDungeon(dungeonName)
                                .orElseThrow();
                        //TODO: Il dungeon non setta al suo avvio il suo maxCompleteTime
                        if (maxCompleteTime != null && unitType != null) {
                            final TimeUnit unit = TimeUnit.valueOf(unitType.toUpperCase());
                            final long elapsedMillis = System.currentTimeMillis() - enterTime.getTime(); //TODO: Pushare i dati dal config in merito al timing altrimenti non funzionerà
                            final long totalMillis = unit.toMillis(maxCompleteTime);
                            final long remainingMillis = totalMillis - elapsedMillis;

                            if (remainingMillis > 0) {

                                final DungeonSession session = createSessionInstance(dungeon, playerId, enterTime.toInstant(), runId);
                                restoreSessionStats(session, deaths, totalKills, damageDealt, damageTaken);

                                sessions.put(playerId, session);
                                dungeonSessions.computeIfAbsent(dungeon, _ -> new CopyOnWriteArrayList<>())
                                        .add(session);

                                timedTasks.put(playerId,
                                        Main.getScheduledExecutor().schedule(() -> {
                                            if (session.isActive()) {
                                                onExpire.accept(playerId);
                                                endSession(playerId, ExitReason.TIME_EXPIRED);
                                            }
                                        }, remainingMillis, TimeUnit.MILLISECONDS));

                                System.out.println("Recovered timed session for player " + playerId +
                                        " with " + (remainingMillis / 1000) + " seconds remaining");
                            } else {
                                System.out.println("Session expired while player was offline: " + playerId);
                                markSessionAsTimeExpired(runId);
                            }
                        } else {
                            final DungeonSession session = createSessionInstance(dungeon, playerId);

                            restoreSessionStats(session, deaths, totalKills, damageDealt, damageTaken);

                            sessions.put(playerId, session);
                            dungeonSessions.computeIfAbsent(dungeon, _ -> new CopyOnWriteArrayList<>())
                                    .add(session);

                            System.out.println("Recovered normal session for player " + playerId);
                        }
                    } else {
                        System.out.println("No active session to recover for player: " + playerId);
                    }
                }

            } catch (SQLException e) {
                System.err.println("Error recovering session for player " + playerId + ": " + e.getMessage());
                throw new RuntimeException("Failed to recover session", e);
            }
        }).exceptionally(ex -> {
            System.err.println("Fatal error during session recovery: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    private void restoreSessionStats(DungeonSession session, int deaths, int kills,
                                     double damageDealt, double damageTaken) {
        for (int i = 0; i < deaths; i++) {
            session.addDeath();
        }
        session.addKill(kills);
        session.addDamage(damageDealt); //TODO: AGGIUNGERE SETDEATH E SETKILLS E FAR FUNZIONARE addKill come addDeath
                                        //TODO: SWAPE! Ti faccio esplodere se non specifici il CachedExecutor nei Futures delle Query!!!
        session.addDamageTaken(damageTaken);
    }

    private void markSessionAsTimeExpired(int runId) {
        String updateSql = """
        UPDATE player_runs 
        SET exit_reason = 'TIME_EXPIRED', exit_time = NOW() 
        WHERE pr_id = ?
        """;

        Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            try (conn; PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setInt(1, runId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Failed to mark session as TIME_EXPIRED: " + e.getMessage());
            }
        });
    }


    public DungeonSession createSessionInstance(final Dungeon dungeon, UUID playerId) {
        return new SessionImpl(playerId, dungeon);
    }

    public DungeonSession createSessionInstance(final Dungeon dungeon, UUID playerId, Instant startTime, final int runId) {
        return new SessionImpl(playerId, dungeon, startTime, runId);
    }

}
