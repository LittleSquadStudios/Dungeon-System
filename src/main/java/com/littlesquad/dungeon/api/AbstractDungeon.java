package com.littlesquad.dungeon.api;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.entrance.EntryResponse;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.event.Event;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.internal.SessionManager;
import com.littlesquad.dungeon.internal.boss.BossRoomManager;
import com.littlesquad.dungeon.internal.checkpoint.CheckPointManager;
import com.littlesquad.dungeon.internal.event.ObjectiveEventImpl;
import com.littlesquad.dungeon.internal.event.StructuralEventImpl;
import com.littlesquad.dungeon.internal.event.TimedEventImpl;
import com.littlesquad.dungeon.internal.file.DungeonParser;
import com.littlesquad.dungeon.internal.utils.CommandUtils;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import net.Indyuce.mmocore.party.provided.Party;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractDungeon implements Dungeon {

    /*When someone fires this command we should add him to this set
    ONLY IF HE'S WITH A PARTY, AND IF HE JOINS WE SHOULD CHECK AGAIN AND EVENTUALLY SET INTO THIS SET*/
    private final Set<UUID> leaders = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Location> joinPoints = new ConcurrentHashMap<>();
    private DungeonParser parser;

    private static final String[] CREATE_TABLES = {
            """
        CREATE TABLE IF NOT EXISTS dungeon(
            dungeon_id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            dungeon_name VARCHAR(30) NOT NULL UNIQUE,
            is_pvp BOOLEAN DEFAULT false
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,

            """
        CREATE TABLE IF NOT EXISTS player(
            player_id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            uuid CHAR(36) NOT NULL UNIQUE,
            INDEX idx_uuid (uuid)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,

            """
        CREATE TABLE IF NOT EXISTS bossroom(
            bossroom_id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            bossroom_name VARCHAR(30) NOT NULL,
            is_timed BOOLEAN DEFAULT false,
            boss_name VARCHAR(30) NOT NULL,
            dungeon_id INT NOT NULL,
            FOREIGN KEY (dungeon_id) REFERENCES dungeon(dungeon_id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,

            """
        CREATE TABLE IF NOT EXISTS objective(
            objective_id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            objective_name VARCHAR(30),
            dungeon_id INT NOT NULL,
            FOREIGN KEY (dungeon_id) REFERENCES dungeon(dungeon_id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,

            """
        CREATE TABLE IF NOT EXISTS time_restrictions(
            restriction_id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            max_complete_time BIGINT NOT NULL,
            unit_type VARCHAR(10) NOT NULL,
            dungeon_id INT NOT NULL UNIQUE,
            FOREIGN KEY (dungeon_id) REFERENCES dungeon(dungeon_id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,

            """
        CREATE TABLE IF NOT EXISTS player_runs(
            pr_id INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
            dungeon_id INT NOT NULL,
            player_id INT NOT NULL,
            deaths INT DEFAULT 0 NOT NULL,
            enter_time DATETIME NOT NULL,
            exit_time DATETIME,
            total_kills INT DEFAULT 0,
            damage_dealt DOUBLE DEFAULT 0,
            damage_taken DOUBLE DEFAULT 0,
            exit_reason ENUM('DEATH', 'TIME_EXPIRED', 'FINISHED', 'QUIT', 'PLUGIN_STOPPING', 'ERROR', 'KICKED') DEFAULT NULL,
            FOREIGN KEY (dungeon_id) REFERENCES dungeon(dungeon_id) ON DELETE CASCADE,
            FOREIGN KEY (player_id) REFERENCES player(player_id) ON DELETE CASCADE,
            INDEX idx_enter_time (enter_time),
            INDEX idx_player_id (player_id),
            INDEX idx_exit_reason (exit_reason)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,

            """
        CREATE TABLE IF NOT EXISTS player_bossroom_defeat(
            pbd_id INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
            player_id INT NOT NULL,
            bossroom_id INT NOT NULL,
            complete_date DATETIME NOT NULL,
            damage_dealt DOUBLE DEFAULT 0,
            damage_taken DOUBLE DEFAULT 0,
            CONSTRAINT fk_defeat_player
                FOREIGN KEY (player_id)
                REFERENCES player(player_id)
                ON DELETE CASCADE,
            CONSTRAINT fk_defeat_bossroom
                FOREIGN KEY (bossroom_id)
                REFERENCES bossroom(bossroom_id)
                ON DELETE CASCADE,
            INDEX idx_complete_date (complete_date)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,

            """
        CREATE TABLE IF NOT EXISTS player_objective_complete(
            poc_id INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
            player_id INT NOT NULL,
            objective_id INT NOT NULL,
            complete_date DATETIME DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT fk_complete_player
                FOREIGN KEY (player_id)
                REFERENCES player(player_id)
                ON DELETE CASCADE,
            CONSTRAINT fk_complete_objective
                FOREIGN KEY (objective_id)
                REFERENCES objective(objective_id)
                ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """
    };

    public AbstractDungeon(final DungeonParser parser) {
        this.parser = parser;
        initializeTables()
                .thenCompose(_ -> insertDungeonIfNotExists())
                .exceptionally(ex -> {
                    Main.getInstance().getLogger().severe("Error initializing dungeon: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    @Override
    public EntryResponse tryEnter(final Player leader) {
        final PlayerData data = Main.getMMOCoreAPI().getPlayerData(leader);
        final Party party = (Party) data.getParty();
        final boolean hasParty = party != null && getOnlinePartySize(party) > 1;
        // Admin permission check
        if (leader.hasPermission(getEntrance().adminPermission())) {
            if (hasParty) {
                return EntryResponse.SUCCESS_PARTY;
            } else {
                return EntryResponse.SUCCESS_SOLO;
            }
        }

        // Check if leader already in dungeon
        final DungeonSession leaderSession = SessionManager.getInstance()
                .getSession(leader.getUniqueId());

        if (leaderSession != null) {
            return EntryResponse.FAILURE_PER_SENDER_ALREADY_IN;
        }

        // Check party members
        if (hasParty) {

            for (final PlayerData onlineMember : party.getOnlineMembers()) {

                final DungeonSession partyMemberSession = SessionManager
                        .getInstance()
                        .getSession(onlineMember.getUniqueId());

                if (partyMemberSession != null) {

                    if (partyMemberSession.getDungeon().id().equals(id())) {
                        return EntryResponse.SUCCESS_SOLO;
                    } else {
                        return EntryResponse.FAILURE_PER_MEMBER_ALREADY_IN;
                    }
                }
            }
        }

        // Check max slots
        if (getEntrance().maxSlots() == 0) {
            return EntryResponse.FAILURE_PER_DUNGEON_BLOCKED;
        }

        // Party required check
        if (getEntrance().partyRequired()) {

            if (!hasParty) {
                return EntryResponse.FAILURE_PER_PARTY;
            }

            if (isPartyAlreadyProcessing(party)) {
                return EntryResponse.FAILURE_PER_ALREADY_PROCESSING;
            }

            final int partySize = getOnlinePartySize(party);

            if (!hasEnoughSlots(partySize, leader)) {
                return EntryResponse.FAILURE_PER_SLOTS;
            }

            if (!hasPartyMinimumLevel(party)) {
                return EntryResponse.FAILURE_PER_LEVEL;
            }

            return EntryResponse.SUCCESS_PARTY;
        }

        // Party not required but player has party
        if (hasParty) {
            if (isPartyAlreadyProcessing(party)) {
                return EntryResponse.FAILURE_PER_ALREADY_PROCESSING;
            }

            final int partySize = getOnlinePartySize(party);

            if (!hasEnoughSlots(partySize, leader)) {
                return EntryResponse.FAILURE_PER_SLOTS;
            }

            if (!hasPartyMinimumLevel(party)) {
                return EntryResponse.FAILURE_PER_LEVEL;
            }

            return EntryResponse.SUCCESS_PARTY;
        }

        // Solo player checks
        if (!hasEnoughSlots(1, leader)) {
            return EntryResponse.FAILURE_PER_SLOTS;
        }

        if (leader.getLevel() < getEntrance().playerMinimumLevel()) {
            return EntryResponse.FAILURE_PER_LEVEL;
        }

        return EntryResponse.SUCCESS_SOLO;
    }

    private boolean isPartyAlreadyProcessing(AbstractParty party) {
        for (final PlayerData pd : party.getOnlineMembers())
            if (leaders.contains(pd.getUniqueId()))
                return true;
        return false;
    }

    private int getOnlinePartySize(AbstractParty party) {
        return party.getOnlineMembers().size();
    }

    private boolean hasEnoughSlots(int incomingPlayers, Player leader) {
        final int maxSlots = getEntrance().maxSlots();

        if (maxSlots == -1)
            return true;

        if (status().currentPlayers() + incomingPlayers <= maxSlots)
            return true;

        return leader.hasPermission(getEntrance().bypassPermission());
    }

    private boolean hasPartyMinimumLevel(AbstractParty party) {
        return party.getOnlineMembers()
                .stream()
                .mapToInt(PlayerData::getLevel)
                .sum() >= getEntrance().partyMinimumLevel();
    }

    @Override
    public void onEnter(final Player player) {

        if (player == null) {
            return;
        }

        final DungeonSession session = SessionManager
                .getInstance()
                .getSession(player.getUniqueId());

        if (session != null) {
            SessionManager
                    .getInstance()
                    .endSession(player.getUniqueId(), ExitReason.KICKED);
        }

        joinPoints.put(player.getUniqueId(), player.getLocation());

        if (isTimed()) {

            SessionManager.getInstance().startTimedSession(this,
                    player.getUniqueId(),
                    getParser().getTimeAmount(),
                    getParser().getTimeUnit(),
                    s -> {
                        final Player p = Bukkit.getPlayer(s);
                        if (p != null) {
                            p.sendMessage("ciao");
                            onExit(p);
                        }
                    });

        } else {
            SessionManager.getInstance()
                    .startSession(this,
                            player.getUniqueId());
        }

        CommandUtils.executeMulti(
                Bukkit.getConsoleSender(),
                getEntrance().onEnterCommands(),
                player);
    }

    @Override
    public void onEnter(final Player... players) {
        if (players == null || players.length == 0) {
            return;
        }

        Arrays.stream(players).forEach(p -> {
            if (p != null) {
                this.onEnter(p);
            }
        });
    }

    @Override
    public void onExit(final Player player) {
        if (player == null) return;

        SessionManager.getInstance()
                .endSession(player.getUniqueId(), ExitReason.QUIT);

        player.teleportAsync(joinPoints.get(player.getUniqueId()));
    }

    @Override
    public void onExit(final Player... players) {
        Arrays.stream(players)
                .toList().forEach(this::onExit);
    }

    @Override
    public void triggerEvent(String eventId, Player triggerer) {
        Optional.ofNullable(getEvent(eventId)).ifPresent(ev -> ev.triggerActivation(triggerer));
    }

    @Override
    public CompletableFuture<Void> triggerEventAsync(String eventId, Player triggerer) {
        return CompletableFuture.runAsync(() -> Optional
                .ofNullable(getEvent(eventId))
                .ifPresent(ev -> ev.triggerActivation(triggerer)));
    }

    @Override
    public void shutdown() {

        SessionManager
                .getInstance()
                .getDungeonSessions(this).forEach(s ->
                    SessionManager
                    .getInstance()
                    .endSession(s.playerId(), ExitReason.PLUGIN_STOPPING));

        Arrays.stream(getCheckpoints()).forEach(checkPoint -> CheckPointManager.unregister(checkPoint.getID()));
        BossRoomManager.getInstance().clear();

        for (final Event event : getEvents()) {
            switch (event.getType()) {
                case TIMED -> ((TimedEventImpl) event).close();
                case OBJECTIVE -> ((ObjectiveEventImpl) event).close();
                case STRUCTURAL -> ((StructuralEventImpl) event).close();
            }
        }

        leaders.clear();
        parser = null;
        status().shutdown();
    }

    private boolean isTimed() {
        return typeFlags().contains(TypeFlag.TIMED);
    }

    public DungeonParser getParser() {
        return parser;
    }

    public static CompletableFuture<Void> initializeTables() {
        return Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            try (conn) {
                conn.setAutoCommit(false);

                try (final Statement stmt = conn.createStatement()) {
                    for (final String sql : CREATE_TABLES) {
                        stmt.executeUpdate(sql);
                    }

                    conn.commit();
                    Main.getInstance().getLogger().info("✓ All database tables created successfully!");

                } catch (final SQLException e) {
                    conn.rollback();
                    Main.getInstance().getLogger().severe("✗ Error creating database tables: " + e.getMessage());
                    throw new RuntimeException("Database initialization failed", e);
                }

            } catch (final SQLException e) {
                Main.getInstance().getLogger().severe("✗ Fatal error with database connection: " + e.getMessage());
                throw new RuntimeException("Database connection error", e);
            }
        });
    }

    private CompletableFuture<Void> insertDungeonIfNotExists() {
        return Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            String insertDungeonQuery = """
            INSERT INTO dungeon (dungeon_name, is_pvp) 
            VALUES (?, ?) 
            ON DUPLICATE KEY UPDATE is_pvp = VALUES(is_pvp)
            """;

            String sqlGetDungeonId = """
            SELECT dungeon_id FROM dungeon WHERE dungeon_name = ?
            """;

            String sqlTimeRestrictions = """
            INSERT INTO time_restrictions (max_complete_time, unit_type, dungeon_id)
            SELECT ?, ?, dungeon_id 
            FROM dungeon 
            WHERE dungeon_name = ?
            ON DUPLICATE KEY UPDATE 
                max_complete_time = VALUES(max_complete_time),
                unit_type = VALUES(unit_type)
            """;

            try (conn) {

                try (final PreparedStatement stmtDungeon = conn.prepareStatement(insertDungeonQuery)) {
                    stmtDungeon.setString(1, id());
                    stmtDungeon.setBoolean(2, typeFlags().contains(TypeFlag.PVP_ENABLED));
                    stmtDungeon.executeUpdate();
                }

                int dungeonId;
                try (final PreparedStatement stmtGet = conn.prepareStatement(sqlGetDungeonId)) {
                    stmtGet.setString(1, id());
                    try (final ResultSet rs = stmtGet.executeQuery()) {
                        if (rs.next()) {
                            dungeonId = rs.getInt("dungeon_id");
                        } else {
                            throw new SQLException("Failed to retrieve dungeon_id for: " + id());
                        }
                    }
                }

                if (isTimed()) {
                    try (final PreparedStatement timeStmt = conn.prepareStatement(sqlTimeRestrictions)) {
                        timeStmt.setLong(1, getParser().getTimeAmount());
                        timeStmt.setString(2, getParser().getTimeUnit().name());
                        timeStmt.setInt(3, dungeonId);
                    }
                }


            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
