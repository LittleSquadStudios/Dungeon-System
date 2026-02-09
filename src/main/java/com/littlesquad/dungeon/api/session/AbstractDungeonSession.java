package com.littlesquad.dungeon.api.session;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.internal.checkpoint.CheckPointManager;

import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractDungeonSession implements DungeonSession {

    private final int runId;
    private final Instant startTime;
    private final AtomicReference<Instant> endTime;
    private final AtomicBoolean active;
    private final AtomicInteger totalKills;
    private final AtomicReference<Double> damageDealt;
    private final AtomicReference<Double> damageTaken;
    private final AtomicInteger deaths;

    private final UUID playerUUID;
    private final String dungeonName;
    private Integer cachedPlayerId;
    private Integer cachedDungeonId;

    private final CountDownLatch idsLoadedLatch = new CountDownLatch(1);

    private final Dungeon dungeon;

    public AbstractDungeonSession(final UUID playerUUID, final Dungeon dungeon, final Instant customStartTime, final int runId) {
        this.playerUUID = playerUUID;
        this.dungeonName = dungeon.id();
        this.dungeon = dungeon;

        this.startTime = customStartTime != null ? customStartTime : Instant.now();
        this.endTime = new AtomicReference<>(null);
        this.active = new AtomicBoolean(true);
        this.totalKills = new AtomicInteger(0);
        this.damageDealt = new AtomicReference<>(0.0);
        this.damageTaken = new AtomicReference<>(0.0);
        this.deaths = new AtomicInteger(0);
        this.runId = runId;

        if (runId > -1) {
            CompletableFuture.allOf(
                    ensurePlayerExists(),
                    ensureDungeonIdLoaded(),
                    loadSessionData()
            ).whenCompleteAsync((_, ex) -> {
                if (ex != null) {
                    ex.printStackTrace();
                } else {
                    Main.getInstance().getLogger().info("Session recovered for player " + playerUUID);
                }
                idsLoadedLatch.countDown();
            }, Main.getCachedExecutor());
        } else {
            idsLoadedLatch.countDown();
        }
    }

    public AbstractDungeonSession(final UUID playerUUID, final Dungeon dungeon) {
        this(playerUUID, dungeon, null, -1);
    }

    @Override
    public void stopSession(final ExitReason reason) {
        if (!active.compareAndSet(true, false)) {
            return;
        }

        if (!(reason.equals(ExitReason.PLUGIN_STOPPING)
                || reason.equals(ExitReason.ERROR)
                || reason.equals(ExitReason.KICKED)))
            endTime.set(Instant.now());

        CompletableFuture<Void> prepareFuture;

        if (runId > -1) {
            prepareFuture = CompletableFuture.runAsync(() -> {
                try {
                    if (!idsLoadedLatch.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timeout waiting for session data to load");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for session data", e);
                }
            }, Main.getCachedExecutor());
        } else {
            prepareFuture = CompletableFuture.allOf(
                    ensurePlayerExists(),
                    ensureDungeonIdLoaded()
            );
        }

        prepareFuture
                .thenRunAsync(() -> pushDatabase(reason), Main.getCachedExecutor())
                .whenCompleteAsync((_, ex) -> {
                    if (ex != null) {
                        ex.printStackTrace();
                    }
                    CheckPointManager.removeCheckPointFor(playerUUID);
                }, Main.getWorkStealingExecutor());
    }

    private CompletableFuture<Void> loadSessionData() {
        return Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            final String select = """
                SELECT deaths, total_kills, damage_dealt, damage_taken
                FROM player_runs
                WHERE pr_id = ?
                """;

            try (conn;
                 final PreparedStatement stmt = conn.prepareStatement(select)) {

                stmt.setInt(1, runId);

                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        deaths.set(rs.getInt("deaths"));
                        totalKills.set(rs.getInt("total_kills"));
                        damageDealt.set(rs.getDouble("damage_dealt"));
                        damageTaken.set(rs.getDouble("damage_taken"));
                    } else {
                        throw new IllegalStateException("Session not found for pr_id: " + runId);
                    }
                }

            } catch (SQLException e) {
                throw new RuntimeException("Error loading session data for pr_id: " + runId, e);
            }
        }, Main.getCachedExecutor());
    }

    private CompletableFuture<Void> ensurePlayerExists() {
        return Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            final String upsert  = """
                    INSERT INTO player (uuid)
                    VALUES (?)
                    ON DUPLICATE KEY UPDATE player_id = LAST_INSERT_ID(player_id)
                    """;

            try (conn;
                 final PreparedStatement stmt = conn.prepareStatement(upsert, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, playerUUID.toString());
                stmt.executeUpdate();

                try (final ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        cachedPlayerId = rs.getInt(1);
                    } else {
                        throw new IllegalStateException("Failed to retrieve player_id for: " + playerUUID);
                    }
                }

            } catch (SQLException e) {
                throw new RuntimeException("Error ensuring player exists for UUID: " + playerUUID, e);
            }
        }, Main.getCachedExecutor());
    }

    private CompletableFuture<Void> ensureDungeonIdLoaded() {
        return Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            String select = "SELECT dungeon_id FROM dungeon WHERE dungeon_name = ?";

            try (conn;
                 PreparedStatement stmt = conn.prepareStatement(select)) {

                stmt.setString(1, dungeonName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        cachedDungeonId = rs.getInt("dungeon_id");
                    } else {
                        throw new IllegalStateException("Dungeon not found: " + dungeonName);
                    }
                }

            } catch (SQLException e) {
                throw new RuntimeException("Error retrieving dungeon_id for: " + dungeonName, e);
            }
        }, Main.getCachedExecutor());
    }

    private void pushDatabase(final ExitReason reason) {
        if (cachedPlayerId == null || cachedDungeonId == null) {
            throw new IllegalStateException("Player ID or Dungeon ID not initialized");
        }

        Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            try (conn) {

                String sql;

                if (runId > -1) {
                    sql = """
                    UPDATE player_runs 
                    SET deaths = ?,
                        exit_time = ?,
                        total_kills = ?,
                        damage_dealt = ?,
                        damage_taken = ?,
                        exit_reason = ?
                    WHERE pr_id = ?
                    """;

                    try (final PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setInt(1, deaths.get());

                        final Instant instant;
                        if ((instant = endTime.get()) == null) {
                            stmt.setNull(2, Types.TIMESTAMP);
                        } else {
                            stmt.setTimestamp(2, Timestamp.from(instant));
                        }

                        stmt.setInt(3, totalKills.get());
                        stmt.setDouble(4, damageDealt.get());
                        stmt.setDouble(5, damageTaken.get());
                        stmt.setString(6, reason.name());
                        stmt.setInt(7, runId);

                        int rows = stmt.executeUpdate();
                    }

                } else {
                    sql = """
                    INSERT INTO player_runs (
                        dungeon_id, player_id, deaths,
                        enter_time, exit_time, total_kills, 
                        damage_dealt, damage_taken, exit_reason
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        deaths = VALUES(deaths),
                        exit_time = VALUES(exit_time),
                        total_kills = VALUES(total_kills),
                        damage_dealt = VALUES(damage_dealt),
                        damage_taken = VALUES(damage_taken),
                        exit_reason = VALUES(exit_reason)
                    """;

                    try (final PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, cachedDungeonId);
                        stmt.setInt(2, cachedPlayerId);
                        stmt.setInt(3, deaths.get());
                        stmt.setTimestamp(4, Timestamp.from(startTime));

                        final Instant instant;
                        if ((instant = endTime.get()) == null) {
                            stmt.setNull(5, Types.TIMESTAMP);
                        } else {
                            stmt.setTimestamp(5, Timestamp.from(instant));
                        }

                        stmt.setInt(6, totalKills.get());
                        stmt.setDouble(7, damageDealt.get());
                        stmt.setDouble(8, damageTaken.get());
                        stmt.setString(9, reason.name());

                        int rows = stmt.executeUpdate();
                    }
                }

            } catch (SQLException e) {
                throw new RuntimeException("Error saving session data", e);
            }
        }, Main.getCachedExecutor()).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    // Getters rimangono invariati
    public Dungeon getDungeon() {
        return dungeon;
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public long timeIn() {
        return ChronoUnit.SECONDS.between(startTime,
                endTime.get() != null ? endTime.get() : Instant.now());
    }

    @Override
    public UUID playerId() {
        return playerUUID;
    }

    @Override
    public Instant enterTime() {
        return startTime;
    }

    @Override
    public Instant exitTime() {
        return endTime.get();
    }

    @Override
    public int kills() {
        return totalKills.get();
    }

    @Override
    public double damageDealt() {
        return damageDealt.get();
    }

    @Override
    public void addKill(int kill) {
        totalKills.addAndGet(kill);
    }

    @Override
    public void addDamage(double damage) {
        damageDealt.updateAndGet(current -> current + damage);
    }

    @Override
    public double damageTaken() {
        return damageTaken.get();
    }

    @Override
    public void addDeath() {
        deaths.incrementAndGet();
    }

    @Override
    public int deaths() {
        return deaths.get();
    }

    @Override
    public void addDamageTaken(double damage) {
        damageTaken.updateAndGet(current -> current + damage);
    }
}