package com.littlesquad.dungeon.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.Credentials;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MySQLConnector {

    private final HikariConfig poolConfig;
    private HikariDataSource dataSource;

    // --------------------------- [ Basic Data ] --------------------------- //

    private String databaseName;
    private int port;
    private String ipAddr;
    private String userName;
    private String password;
    private boolean sslConnection = false;

    // --------------------------- [ Complex Options ] ---------------------------//

    private int maximumPoolSize = 1;
    private int minimumIdle = 0;
    private long connectionTimeout = 1000;
    private long idleTimeout = 0;
    private long maxLifetime = 30000;
    private boolean autoCommit = false;
    private long leakDetectionThreshold = 0;

    private final ExecutorService connectionGetter;


    public MySQLConnector() {
        this(Executors.newCachedThreadPool());
    }
    public MySQLConnector(final ExecutorService ex) {
        poolConfig = new HikariConfig();
        connectionGetter = ex;
    }

    public MySQLConnector(final String databaseName,
                          final String ipAddr,
                          final int port,
                          final String userName,
                          final String password) {
        this(databaseName, ipAddr, port, userName, password, Executors.newCachedThreadPool());
    }
    public MySQLConnector(final String databaseName,
                          final String ipAddr,
                          final int port,
                          final String userName,
                          final String password,
                          final ExecutorService ex) {
        poolConfig = new HikariConfig();
        poolConfig.setDriverClassName("org.mariadb.jdbc.Driver");
        poolConfig.setCredentials(Credentials.of(userName, password));
        poolConfig.addDataSourceProperty("serverName", ipAddr);
        poolConfig.addDataSourceProperty("portNumber", port);
        poolConfig.addDataSourceProperty("databaseName", databaseName);

        poolConfig.setConnectionTimeout(5000);

        this.dataSource = new HikariDataSource(poolConfig);
        connectionGetter = ex;
    }

    public MySQLConnector(final String url) {
        this(url, Executors.newCachedThreadPool());
    }
    public MySQLConnector(final String url,
                          final ExecutorService ex) {
        poolConfig = new HikariConfig();
        poolConfig.setDriverClassName("org.mariadb.jdbc.Driver");
        if (url.startsWith("jdbc:mariadb://"))
            poolConfig.setJdbcUrl(url);
        this.dataSource = new HikariDataSource(poolConfig);
        connectionGetter = ex;
    }

    // --------------------------- [ SETTERS ] --------------------------- //

    public MySQLConnector setMinimumIdle(int minimumIdle) {
        this.minimumIdle = minimumIdle;
        return this;
    }

    public MySQLConnector setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
        return this;
    }

    public MySQLConnector setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public MySQLConnector setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }

    public MySQLConnector setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
        return this;
    }

    public MySQLConnector setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
        return this;
    }

    public MySQLConnector setLeakDetectionThreshold(long leakDetectionThreshold) {
        this.leakDetectionThreshold = leakDetectionThreshold;
        return this;
    }

    public MySQLConnector setDatabaseName (final String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    public MySQLConnector setPort(int port) {
        this.port = port;
        return this;
    }

    public MySQLConnector setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
        return this;
    }

    public MySQLConnector setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public MySQLConnector setPassword(String password) {
        this.password = password;
        return this;
    }

    public MySQLConnector setSslConnection(boolean sslConnection) {
        this.sslConnection = sslConnection;
        return this;
    }

    private void validateBasicParameters() {
        if (ipAddr == null || ipAddr.isEmpty())
            throw new IllegalArgumentException("IP address cannot be null or empty");

        if (port <= 0 || port > 65535)
            throw new IllegalArgumentException("Port must be between 1 and 65535");

        if (userName == null || userName.isEmpty())
            throw new IllegalArgumentException("Username cannot be null or empty");

        if (password == null)
            throw new IllegalArgumentException("Password cannot be null");
    }

    private void validatePoolParameters() {
        if (maximumPoolSize < 1)
            throw new IllegalArgumentException("Maximum pool size must be at least 1");

        if (minimumIdle < 0)
            throw new IllegalArgumentException("Minimum idle cannot be negative");

        if (minimumIdle > maximumPoolSize)
            throw new IllegalArgumentException("Minimum idle cannot exceed maximum pool size");

        if (connectionTimeout < 1000)
            throw new IllegalArgumentException("Connection timeout must be >= 1000ms");

        if (maxLifetime < 30000)
            throw new IllegalArgumentException("Max lifetime must be >= 30000ms (30s)");

        if (leakDetectionThreshold < 0)
            throw new IllegalArgumentException("Leak detection threshold cannot be negative");
    }

    private void checkDataSource() {
        if (dataSource == null || dataSource.isClosed())
            throw new IllegalStateException("DataSource is not initialized or has been closed");
    }

    public void buildDataSource () {

        validateBasicParameters();
        validatePoolParameters();

        poolConfig.setDriverClassName("org.mariadb.jdbc.Driver");
        poolConfig.setCredentials(Credentials.of(userName, password));

        poolConfig.addDataSourceProperty("serverName", ipAddr);
        poolConfig.addDataSourceProperty("portNumber", port);
        poolConfig.addDataSourceProperty("databaseName", databaseName);

        poolConfig.setMaximumPoolSize(maximumPoolSize);
        poolConfig.setConnectionTimeout(connectionTimeout);
        poolConfig.setMinimumIdle(minimumIdle);
        poolConfig.setIdleTimeout(idleTimeout);
        poolConfig.setMaxLifetime(maxLifetime);
        poolConfig.setAutoCommit(autoCommit);
        poolConfig.setLeakDetectionThreshold(leakDetectionThreshold);

        if (dataSource != null && !dataSource.isClosed())
            dataSource.close();
        this.dataSource = new HikariDataSource(poolConfig);
    }

    // --------------------------- [ GET CONNECTION ] --------------------------- //

    public CompletableFuture<Connection> getConnection (final int triesAmount) {
        checkDataSource();

        return CompletableFuture.supplyAsync(() -> {
            int tries = triesAmount;

            while (tries-- > 0) {
                try {
                    return dataSource.getConnection();
                } catch (SQLException e) {
                    System.out.println("Connection failed: " + e.getMessage());
                }
            }
            return null;
        }, connectionGetter);
    }

    // --------------------------- [ CLOSE ] --------------------------- //

    public void close () {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        connectionGetter.shutdown();
    }
}
