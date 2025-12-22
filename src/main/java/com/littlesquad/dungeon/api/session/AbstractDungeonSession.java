package com.littlesquad.dungeon.api.session;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;

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

    private static final ScheduledExecutorService scheduler;
    private static final ExecutorService executor;
    private ScheduledFuture<?> saveTask;

    static {
        scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        executor = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
    }

    private final UUID playerUUID;
    private final String dungeonName;
    private Integer cachedPlayerId;
    private Integer cachedDungeonId;

    public AbstractDungeonSession(final UUID playerUUID, final Dungeon dungeon) {
        this.playerUUID = playerUUID;
        this.dungeonName = dungeon.id();

        this.startTime = Instant.now();
        this.endTime = new AtomicReference<>(null);
        this.active = new AtomicBoolean(false);
        this.totalKills = new AtomicInteger(0);
        this.damageDealt = new AtomicReference<>(0.0);
        this.damageTaken = new AtomicReference<>(0.0);
        this.deaths = new AtomicInteger(0);

        active.set(true);

        CompletableFuture.allOf(
                ensurePlayerExists(),
                ensureDungeonIdLoaded()
        ).thenRunAsync(this::startPeriodicSave, executor)
        .handleAsync((_, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
            }
            return null;
        }, executor);
    }

    @Override
    public void stopSession() {
        active.set(false);
        endTime.set(Instant.now());

        if (saveTask != null) {
            saveTask.cancel(false);
        }

        updateRecord();
        shutdownExecutors();
    }

    private CompletableFuture<Void> ensurePlayerExists() {
        return Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            try {
                final String insert = "INSERT IGNORE INTO player (uuid) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                    stmt.setString(1, playerUUID.toString());
                    stmt.executeUpdate();
                }

                String select = "SELECT player_id FROM player WHERE uuid = ?";
                try (final PreparedStatement stmt = conn.prepareStatement(select)) {
                    stmt.setString(1, playerUUID.toString());
                    final ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        cachedPlayerId = rs.getInt("player_id");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Error while retrieving player_id", e);
            }
        }, executor);
    }

    private CompletableFuture<Void> ensureDungeonIdLoaded() {
        return Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            try {
                String select = "SELECT dungeon_id FROM dungeon WHERE dungeon_name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(select)) {
                    stmt.setString(1, dungeonName);
                    final ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        cachedDungeonId = rs.getInt("dungeon_id");
                    } else {
                        throw new IllegalStateException("Dungeon not found for signature: " + dungeonName);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Error while retrieving dungeon_id", e);
            }
        }, executor);
    }

    private void startPeriodicSave() {
        saveTask = scheduler.scheduleAtFixedRate(() -> {
            if (active.get()) {
                updateRecord();
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    public void insertOrUpdateRecord() {
        if (cachedPlayerId == null || cachedDungeonId == null) {
            throw new IllegalStateException("Player ID o Dungeon ID non inizializzati");
        }

        final String sql = "INSERT INTO player_runs (dungeon_id, player_id, deaths, " +
                "enter_time, exit_time, total_kills, damage_dealt, damage_taken) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "deaths = VALUES(deaths), " +
                "enter_time = VALUES(enter_time), " +
                "exit_time = VALUES(exit_time), " +
                "total_kills = VALUES(total_kills), " +
                "damage_dealt = VALUES(damage_dealt), " +
                "damage_taken = VALUES(damage_taken)";

        Main.getConnector()
                .getConnection(10)
                .thenAcceptAsync(conn -> {
                    try (final PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, cachedDungeonId);
                        stmt.setInt(2, cachedPlayerId);
                        stmt.setInt(3, deaths.get());
                        stmt.setTimestamp(4, Timestamp.from(startTime));
                        stmt.setTimestamp(5, Timestamp.from(Instant.now()));
                        stmt.setInt(6, totalKills.get());
                        stmt.setDouble(7, damageDealt.get());
                        stmt.setDouble(8, damageTaken.get());

                        stmt.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }, executor).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    private void updateRecord() {
        if (cachedPlayerId == null || cachedDungeonId == null) {
            return;
        }

        final String sql = "UPDATE player_runs SET deaths = ?, exit_time = ?, " +
                "total_kills = ?, damage_dealt = ?, damage_taken = ? " +
                "WHERE dungeon_id = ? AND player_id = ?";

        Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, deaths.get());
                stmt.setTimestamp(2, Timestamp.from(endTime.get() != null ? endTime.get() : Instant.now()));
                stmt.setInt(3, totalKills.get());
                stmt.setDouble(4, damageDealt.get());
                stmt.setDouble(5, damageTaken.get());
                stmt.setInt(6, cachedDungeonId);
                stmt.setInt(7, cachedPlayerId);

                int updated = stmt.executeUpdate();
                if (updated == 0) {
                    insertOrUpdateRecord();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, executor).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
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
    public Long runId() {
        return null;
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
        return damageTaken.getPlain();
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

    public static void shutdownExecutors() {
        scheduler.shutdown();
        executor.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}