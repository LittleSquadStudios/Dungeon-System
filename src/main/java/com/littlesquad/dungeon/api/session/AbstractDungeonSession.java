package com.littlesquad.dungeon.api.session;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.internal.checkpoint.CheckPointManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractDungeonSession implements DungeonSession {

    private final Instant startTime;
    private final AtomicReference<Instant> endTime;
    private final AtomicBoolean active;
    private final AtomicInteger totalKills;
    private final AtomicReference<Double> damageDealt;
    private final AtomicReference<Double> damageTaken;
    private final AtomicInteger deaths;

    private static final ExecutorService executor;

    static {
        executor = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
    }

    private final UUID playerUUID;
    private final String dungeonName;
    private Integer cachedPlayerId;
    private Integer cachedDungeonId;

    private final Dungeon dungeon;

    public AbstractDungeonSession(final UUID playerUUID, final Dungeon dungeon, Instant customStartTime) {
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
    }

    public AbstractDungeonSession(final UUID playerUUID, final Dungeon dungeon) {
        this(playerUUID, dungeon, null);
    }

    @Override
    public void stopSession(final ExitReason reason) {
        if (!active.compareAndSet(true, false)) {
            System.out.println("Session already stopped for: " + playerUUID);
            return;
        }

        if (!(reason.equals(ExitReason.PLUGIN_STOPPING)
                || reason.equals(ExitReason.ERROR)
                || reason.equals(ExitReason.KICKED)))
            endTime.set(Instant.now());

        CompletableFuture.allOf(
                        ensurePlayerExists(),
                        ensureDungeonIdLoaded()
                )
                .thenRunAsync(() -> pushDatabase(reason), executor)
                .whenCompleteAsync((_, ex) -> {
                    if (ex != null) {
                        System.err.println("Error saving session for " + playerUUID + ": " + ex.getMessage());
                        ex.printStackTrace();
                    } else {
                        System.out.println("Session saved successfully for: " + playerUUID);
                    }
                    CheckPointManager.removeCheckPointFor(playerUUID);
                }, executor);

    }

    private CompletableFuture<Void> ensurePlayerExists() {
        return Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            final String insert = "INSERT IGNORE INTO player (uuid) VALUES (?)";
            final String select = "SELECT player_id FROM player WHERE uuid = ?";

            try (conn;
                 final PreparedStatement insertStmt = conn.prepareStatement(insert);
                 final PreparedStatement selectStmt = conn.prepareStatement(select)) {

                // Insert player
                insertStmt.setString(1, playerUUID.toString());
                insertStmt.executeUpdate();

                // Get player_id
                selectStmt.setString(1, playerUUID.toString());
                try (final ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        cachedPlayerId = rs.getInt("player_id");
                    } else {
                        throw new IllegalStateException("Player not found after insert: " + playerUUID);
                    }
                }

            } catch (SQLException e) {
                throw new RuntimeException("Error ensuring player exists for UUID: " + playerUUID, e);
            }
        }, executor);
    }

    private CompletableFuture<Void> ensureDungeonIdLoaded() {
        return Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            String select = "SELECT dungeon_id FROM dungeon WHERE dungeon_name = ?";

            try (conn;
                 PreparedStatement stmt = conn.prepareStatement(select)) {

                stmt.setString(1, dungeonName); // PRIMA setti il parametro

                try (ResultSet rs = stmt.executeQuery()) { // POI esegui la query
                    if (rs.next()) {
                        cachedDungeonId = rs.getInt("dungeon_id");
                    } else {
                        throw new IllegalStateException("Dungeon not found: " + dungeonName);
                    }
                }

            } catch (SQLException e) {
                throw new RuntimeException("Error retrieving dungeon_id for: " + dungeonName, e);
            }
        }, executor);
    }

    private void pushDatabase(final ExitReason reason) {
        if (cachedPlayerId == null || cachedDungeonId == null) {
            throw new IllegalStateException("Player ID or Dungeon ID not initialized");
        }

        String sql = """
        INSERT INTO player_runs (dungeon_id, player_id, deaths,
            enter_time, exit_time, total_kills, damage_dealt, damage_taken, exit_reason)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            try (conn;
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, cachedDungeonId);
                stmt.setInt(2, cachedPlayerId);
                stmt.setInt(3, deaths.get());
                stmt.setTimestamp(4, Timestamp.from(startTime));
                stmt.setTimestamp(5, Timestamp.from(endTime.get()));
                stmt.setInt(6, totalKills.get());
                stmt.setDouble(7, damageDealt.get());
                stmt.setDouble(8, damageTaken.get());
                stmt.setString(9, reason.name());

                int rows = stmt.executeUpdate();
                System.out.println("Saved session data: " + rows + " row(s) affected");

            } catch (SQLException e) {
                throw new RuntimeException("Error saving session data", e);
            }
        }, executor).exceptionally(ex -> {
            System.err.println("Database push failed: " + ex.getMessage());
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

    public static void shutdownExecutor() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}