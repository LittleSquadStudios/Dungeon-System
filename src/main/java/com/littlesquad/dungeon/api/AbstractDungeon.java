package com.littlesquad.dungeon.api;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.entrance.EntryResponse;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.internal.SessionManager;
import com.littlesquad.dungeon.internal.file.DungeonParser;
import com.littlesquad.dungeon.internal.utils.CommandUtils;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
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
import java.util.List;
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
        System.out.println("=== DEBUG tryEnter START ===");
        System.out.println("Leader: " + leader.getName());

        final PlayerData data = Main.getMMOCoreAPI().getPlayerData(leader);
        final Party party = (Party) data.getParty();
        System.out.println("Party object: " + (party != null ? "EXISTS" : "NULL"));

        final boolean hasParty = party != null && getOnlinePartySize(party) > 1;
        System.out.println("hasParty: " + hasParty);
        if (party != null) {
            System.out.println("Online party size: " + getOnlinePartySize(party));
        }

        // Admin permission check
        System.out.println("Debug checkpoint 1 - Admin check");
        System.out.println("Has admin permission: " + leader.hasPermission(getEntrance().adminPermission()));
        if (leader.hasPermission(getEntrance().adminPermission())) {
            System.out.println("Debug checkpoint 2 - Admin confirmed");
            if (hasParty) {
                System.out.println("Admin with party -> SUCCESS_PARTY");
                return EntryResponse.SUCCESS_PARTY;
            } else {
                System.out.println("Admin solo -> SUCCESS_SOLO");
                return EntryResponse.SUCCESS_SOLO;
            }
        }

        // Check if leader already in dungeon
        System.out.println("Debug checkpoint 3 - Check leader session");
        final DungeonSession leaderSession = SessionManager.getInstance()
                .getSession(leader.getUniqueId());
        System.out.println("Leader has active session: " + (leaderSession != null));

        if (leaderSession != null) {
            System.out.println("Debug checkpoint 4 - Leader already in dungeon");
            System.out.println("Leader's dungeon ID: " + leaderSession.getDungeon().id());
            return EntryResponse.FAILURE_PER_SENDER_ALREADY_IN;
        }

        // Check party members
        System.out.println("Debug checkpoint 5 - Party members check");
        if (hasParty) {
            System.out.println("Debug checkpoint 6 - Has party, checking members");
            System.out.println("Party online members count: " + party.getOnlineMembers().size());

            for (final PlayerData onlineMember : party.getOnlineMembers()) {
                System.out.println("Debug checkpoint 7 - Checking member: " + onlineMember.getPlayer().getName());

                final DungeonSession partyMemberSession = SessionManager
                        .getInstance()
                        .getSession(onlineMember.getUniqueId());

                System.out.println("Debug checkpoint 8 - Member session: " + (partyMemberSession != null ? "EXISTS" : "NULL"));

                if (partyMemberSession != null) {
                    System.out.println("Debug checkpoint 9 - Member in dungeon");
                    System.out.println("Member's dungeon ID: " + partyMemberSession.getDungeon().id());
                    System.out.println("Current dungeon ID: " + id());
                    System.out.println("Same dungeon? " + partyMemberSession.getDungeon().id().equals(id()));

                    if (partyMemberSession.getDungeon().id().equals(id())) {
                        System.out.println("Debug checkpoint 10 - Member in SAME dungeon -> SUCCESS_SOLO");
                        return EntryResponse.SUCCESS_SOLO;
                    } else {
                        System.out.println("Debug checkpoint 11 - Member in DIFFERENT dungeon -> FAILURE");
                        return EntryResponse.FAILURE_PER_MEMBER_ALREADY_IN;
                    }
                }
            }
            System.out.println("All party members checked, none in dungeon");
        }

        // Check max slots
        System.out.println("Debug checkpoint 12 - Max slots check");
        System.out.println("Max slots: " + getEntrance().maxSlots());
        if (getEntrance().maxSlots() == 0) {
            System.out.println("Dungeon blocked (0 slots) -> FAILURE");
            return EntryResponse.FAILURE_PER_DUNGEON_BLOCKED;
        }

        // Party required check
        System.out.println("Debug checkpoint 13 - Party required check");
        System.out.println("Party required: " + getEntrance().partyRequired());
        if (getEntrance().partyRequired()) {
            System.out.println("Debug checkpoint 14 - Party IS required");

            if (!hasParty) {
                System.out.println("No party but required -> FAILURE_PER_PARTY");
                return EntryResponse.FAILURE_PER_PARTY;
            }

            System.out.println("Debug checkpoint 15 - Checking if party already processing");
            if (isPartyAlreadyProcessing(party)) {
                System.out.println("Party already processing -> FAILURE");
                return EntryResponse.FAILURE_PER_ALREADY_PROCESSING;
            }

            final int partySize = getOnlinePartySize(party);
            System.out.println("Debug checkpoint 16 - Party size: " + partySize);
            System.out.println("Has enough slots: " + hasEnoughSlots(partySize, leader));

            if (!hasEnoughSlots(partySize, leader)) {
                System.out.println("Not enough slots -> FAILURE");
                return EntryResponse.FAILURE_PER_SLOTS;
            }

            System.out.println("Debug checkpoint 17 - Checking party level");
            System.out.println("Has minimum level: " + hasPartyMinimumLevel(party));
            if (!hasPartyMinimumLevel(party)) {
                System.out.println("Party level too low -> FAILURE");
                return EntryResponse.FAILURE_PER_LEVEL;
            }

            System.out.println("All checks passed -> SUCCESS_PARTY");
            return EntryResponse.SUCCESS_PARTY;
        }

        // Party not required but player has party
        System.out.println("Debug checkpoint 18 - Party not required, checking if has party");
        if (hasParty) {
            System.out.println("Debug checkpoint 19 - Player has party (optional)");

            System.out.println("Checking if party already processing");
            if (isPartyAlreadyProcessing(party)) {
                System.out.println("Party already processing -> FAILURE");
                return EntryResponse.FAILURE_PER_ALREADY_PROCESSING;
            }

            final int partySize = getOnlinePartySize(party);
            System.out.println("Debug checkpoint 20 - Party size: " + partySize);
            System.out.println("Has enough slots: " + hasEnoughSlots(partySize, leader));

            if (!hasEnoughSlots(partySize, leader)) {
                System.out.println("Not enough slots -> FAILURE");
                return EntryResponse.FAILURE_PER_SLOTS;
            }

            System.out.println("Debug checkpoint 21 - Checking party level");
            System.out.println("Has minimum level: " + hasPartyMinimumLevel(party));
            if (!hasPartyMinimumLevel(party)) {
                System.out.println("Party level too low -> FAILURE");
                return EntryResponse.FAILURE_PER_LEVEL;
            }

            System.out.println("Party checks passed -> SUCCESS_PARTY");
            return EntryResponse.SUCCESS_PARTY;
        }

        // Solo player checks
        System.out.println("Debug checkpoint 22 - Solo player checks");
        System.out.println("Has enough slots for 1: " + hasEnoughSlots(1, leader));
        if (!hasEnoughSlots(1, leader)) {
            System.out.println("Not enough slots for solo -> FAILURE");
            return EntryResponse.FAILURE_PER_SLOTS;
        }

        System.out.println("Debug checkpoint 23 - Level check");
        System.out.println("Player level: " + leader.getLevel());
        System.out.println("Required level: " + getEntrance().playerMinimumLevel());
        if (leader.getLevel() < getEntrance().playerMinimumLevel()) {
            System.out.println("Level too low -> FAILURE");
            return EntryResponse.FAILURE_PER_LEVEL;
        }

        System.out.println("All solo checks passed -> SUCCESS_SOLO");
        System.out.println("=== DEBUG tryEnter END ===");
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

        System.out.println("Current players: " + status().currentPlayers());
        System.out.println("Enough slots?: " + (status().currentPlayers() + incomingPlayers <= maxSlots));

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
    public CompletableFuture<EntryResponse> tryEnterAsync(Player p) {
        // TODO: Lascio l'onere a draky, saprei come farlo ma non voglio urla addosso ;)
        return null;
    }

    @Override
    public void onEnter(final Player player) {
        System.out.println("=== DEBUG onEnter(Player) START ===");
        System.out.println("Player: " + (player != null ? player.getName() : "NULL"));

        if (player == null) {
            System.out.println("Player is null -> returning");
            return;
        }

        System.out.println("Player UUID: " + player.getUniqueId());

        final DungeonSession session = SessionManager
                .getInstance()
                .getSession(player.getUniqueId());

        System.out.println("Existing session: " + (session != null ? session.getDungeon().id() : "NONE"));

        if (session != null) {
            System.out.println("Found existing session, ending with KICKED reason");
            SessionManager
                    .getInstance()
                    .endSession(player.getUniqueId(), ExitReason.KICKED);
            System.out.println("Existing session ended");
        }

        System.out.println("Saving join point location: " + player.getLocation());
        joinPoints.put(player.getUniqueId(), player.getLocation());

        System.out.println("Is timed dungeon: " + isTimed());
        if (isTimed()) {
            System.out.println("Starting TIMED session");
            System.out.println("Time amount: " + getParser().getTimeAmount());
            System.out.println("Time unit: " + getParser().getTimeUnit());

            SessionManager.getInstance().startTimedSession(this,
                    player.getUniqueId(),
                    getParser().getTimeAmount(),
                    getParser().getTimeUnit(),
                    s -> {
                        System.out.println("Timed session callback triggered for UUID: " + s);
                        final Player p = Bukkit.getPlayer(s);
                        if (p != null) {
                            System.out.println("Player found: " + p.getName() + ", sending message and calling onExit");
                            p.sendMessage("ciao");
                            onExit(p);
                        } else {
                            System.out.println("WARNING: Player not found for UUID: " + s);
                        }
                    });
            System.out.println("Timed session started");

        } else {
            System.out.println("Starting STANDARD session");
            SessionManager.getInstance()
                    .startSession(this,
                            player.getUniqueId());
            System.out.println("Standard session started");
        }

        System.out.println("OnEnter commands: " + getEntrance().onEnterCommands());
        System.out.println("Executing onEnter commands...");

        CommandUtils.executeMulti(
                Bukkit.getConsoleSender(),
                getEntrance().onEnterCommands(),
                player);

        System.out.println("OnEnter commands executed successfully");
        System.out.println("=== DEBUG onEnter(Player) END ===");
    }

    @Override
    public void onEnter(final Player... players) {
        System.out.println("=== DEBUG onEnter(Player...) START ===");
        System.out.println("Players array: " + (players != null ? "EXISTS" : "NULL"));
        System.out.println("Players array length: " + (players != null ? players.length : "N/A"));

        if (players == null || players.length == 0) {
            System.out.println("WARNING: Players array is null or empty -> returning");
            return;
        }

        System.out.println("Players in array:");
        for (int i = 0; i < players.length; i++) {
            System.out.println("  [" + i + "] " + (players[i] != null ? players[i].getName() + " (UUID: " + players[i].getUniqueId() + ")" : "NULL"));
        }

        System.out.println("Processing each player with stream...");
        Arrays.stream(players).forEach(p -> {
            if (p != null) {
                System.out.println("Stream processing player: " + p.getName());
                this.onEnter(p);
                System.out.println("Completed processing: " + p.getName());
            } else {
                System.out.println("WARNING: Null player in stream, skipping");
            }
        });

        System.out.println("=== DEBUG onEnter(Player...) END ===");
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
        Arrays.stream(getEvents())
                .filter(event -> event.getID().equals(eventId))
                .findFirst()
                .ifPresent(ev -> ev.triggerActivation(triggerer));

    }

    @Override
    public CompletableFuture<Void> triggerEventAsync(String eventId, Player triggerer) {
        return CompletableFuture.runAsync(() ->
                Arrays.stream(getEvents())
                    .filter(event -> event.getID().equals(eventId))
                    .findFirst()
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
