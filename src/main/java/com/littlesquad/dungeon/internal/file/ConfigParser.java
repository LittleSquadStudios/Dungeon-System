package com.littlesquad.dungeon.internal.file;

import com.littlesquad.Main;
import com.littlesquad.dungeon.database.MySQLConnector;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ConfigParser {

    private final FileConfiguration config;

    ConfigParser(final FileConfiguration config) {
        this.config = config;
    }

    public MySQLConnector createConnector() {
        if (!config.getBoolean("database.enabled", false)) {
            Main.getDungeonLogger().warning("Database is disabled. Player data will not be saved.");
            return null;
        }

        final String databaseName = config.getString("database.name");
        final String databaseIp = config.getString("database.ip-address");
        final int databasePort = config.getInt("database.port", 0);
        final String databaseUsername = config.getString("database.username");
        final String databasePassword = config.getString("database.password");
        final boolean usingSsl = config.getBoolean("database.ssl", false);

        if (databaseName == null ||
                databaseIp == null ||
                databaseUsername == null ||
                databasePassword == null ||
                databasePort <= 0) {

            Main.getDungeonLogger().severe("Database is enabled but credentials are missing or invalid in config.yml");
            return null;
        }

        final boolean cached = config.getBoolean("database.connection_getter.is_cached", true);

        final ExecutorService executor;
        if (cached) {
            executor = Main.getCachedExecutor();
        } else {
            final int threadCount = config.getInt("database.connection_getter.thread_count", 4);
            final boolean workStealing = config.getBoolean("database.connection_getter.is_work_stealing", false);

            executor = workStealing
                    ? Executors.newWorkStealingPool(threadCount)
                    : Executors.newFixedThreadPool(threadCount);
        }

        try {
            final MySQLConnector connector = new MySQLConnector(executor);

            connector
                    .setDatabaseName(databaseName)
                    .setIpAddr(databaseIp)
                    .setPort(databasePort)
                    .setUserName(databaseUsername)
                    .setPassword(databasePassword)
                    .setSslConnection(usingSsl)
                    .setMaximumPoolSize(config.getInt("database.maximum-pool_size", 10))
                    .setMinimumIdle(config.getInt("database.minimum-idle", 2))
                    .setConnectionTimeout(config.getLong("database.connection-timeout", 30000))
                    .setIdleTimeout(config.getLong("database.idle-timeout", 600000))
                    .setMaxLifetime(config.getLong("database.max-lifetime", 1800000))
                    .setAutoCommit(config.getBoolean("database.auto-commit", true))
                    .setLeakDetectionThreshold(config.getLong("database.leak-detection-threshold", 0))
                    .buildDataSource();

            return connector;

        } catch (Exception e) {
            Main.getDungeonLogger().severe("Failed to create MySQLConnector: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}