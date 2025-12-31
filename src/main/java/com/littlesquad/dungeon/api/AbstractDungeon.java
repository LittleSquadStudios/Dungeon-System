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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

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
            dungeon_id INT NOT NULL,
            player_id INT NOT NULL,
            deaths INT DEFAULT 0 NOT NULL,
            enter_time DATETIME NOT NULL,
            exit_time DATETIME,
            total_kills INT DEFAULT 0,
            damage_dealt DOUBLE DEFAULT 0,
            damage_taken DOUBLE DEFAULT 0,
            PRIMARY KEY(dungeon_id, player_id),
            FOREIGN KEY (dungeon_id) REFERENCES dungeon(dungeon_id) ON DELETE CASCADE,
            FOREIGN KEY (player_id) REFERENCES player(player_id) ON DELETE CASCADE,
            INDEX idx_enter_time (enter_time),
            INDEX idx_player_id (player_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """,

            """
        CREATE TABLE IF NOT EXISTS player_bossroom_defeat(
            player_id INT NOT NULL,
            bossroom_id INT NOT NULL,
            complete_date DATETIME NOT NULL,
            damage_dealt DOUBLE DEFAULT 0,
            damage_taken DOUBLE DEFAULT 0,
            PRIMARY KEY (player_id, bossroom_id),
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
            player_id INT NOT NULL,
            objective_id INT NOT NULL,
            complete_date DATETIME DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (player_id, objective_id),
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

        // Inizializza le tabelle e poi inserisci il dungeon
        initializeTables()
                .thenCompose(v -> insertDungeonIfNotExists())
                .exceptionally(ex -> {
                    Main.getInstance().getLogger().severe("Error initializing dungeon: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    public static CompletableFuture<Void> initializeTables() {
        return Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            try {
                conn.setAutoCommit(false);

                try (Statement stmt = conn.createStatement()) {
                    for (String sql : CREATE_TABLES) {
                        stmt.executeUpdate(sql);
                    }

                    conn.commit();
                    Main.getInstance().getLogger().info("✓ All database tables created successfully!");

                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }

            } catch (SQLException e) {
                Main.getInstance().getLogger().severe("✗ Error creating database tables: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private CompletableFuture<Void> insertDungeonIfNotExists() {
        return Main.getConnector().getConnection(10).thenAcceptAsync(conn -> {
            String sql = "INSERT INTO dungeon (dungeon_name, is_pvp) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE is_pvp = VALUES(is_pvp)";

            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id());
                stmt.setBoolean(2, typeFlags().contains(TypeFlag.PVP_ENABLED));
                stmt.executeUpdate();

                Main.getInstance().getLogger().info("Dungeon '" + id() + "' registered in database");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public EntryResponse tryEnter(final Player leader) {

        final PlayerData data = Main.getMMOCoreAPI().getPlayerData(leader);
        final AbstractParty party = data.getParty();

        final boolean hasParty = party != null && getOnlinePartySize(party) > 1;

        if (leader.hasPermission(getEntrance().adminPermission())) {
            if (hasParty)
                return EntryResponse.SUCCESS_PARTY;
            else
                return EntryResponse.SUCCESS_SOLO;
        }

        System.out.println(SessionManager.getInstance()
                .getSession(leader.getUniqueId()) != null);

        if (SessionManager.getInstance()
                .getSession(leader.getUniqueId()) != null)
            return EntryResponse.FAILURE_PER_SENDER_ALREADY_IN;

        if (hasParty) {
            for (final PlayerData onlineMember : party.getOnlineMembers()) {
                final DungeonSession partyMemberSession = SessionManager
                        .getInstance()
                        .getSession(onlineMember.getUniqueId());

                if (partyMemberSession != null) {
                    if (partyMemberSession.getDungeon() == this)
                        return EntryResponse.SUCCESS_SOLO;
                    else
                        return EntryResponse.FAILURE_PER_MEMBER_ALREADY_IN;
                }

            }
        }


        // New check: If the player has adminPermission he can join without any other check


        if (getEntrance().maxSlots() == 0) {
            return EntryResponse.FAILURE_PER_DUNGEON_BLOCKED;
        }

        // First check: Are the party required? Is the player alone?


        if (getEntrance().partyRequired()) {

            // Second check: Check if leader has a party if not, it will return FAILURE_PER_PARTY

            if (!hasParty) {
                return EntryResponse.FAILURE_PER_PARTY;
            }

            // Third check:

            if (isPartyAlreadyProcessing(party))
                return EntryResponse.FAILURE_PER_ALREADY_PROCESSING;

            final int partySize = getOnlinePartySize(party);

            if(!hasEnoughSlots(partySize, leader)) {
                return EntryResponse.FAILURE_PER_SLOTS;
            }

            if (!hasPartyMinimumLevel(party)) {
                return EntryResponse.FAILURE_PER_LEVEL;
            }

            return EntryResponse.SUCCESS_PARTY;
        }

        if (hasParty) {

            if (isPartyAlreadyProcessing(party))
                return EntryResponse.FAILURE_PER_ALREADY_PROCESSING;

            final int partySize = getOnlinePartySize(party);

            if (!hasEnoughSlots(partySize, leader)) {
                return EntryResponse.FAILURE_PER_SLOTS;
            }

            if (!hasPartyMinimumLevel(party)) {
                return EntryResponse.FAILURE_PER_LEVEL;
            }

            return EntryResponse.SUCCESS_PARTY;
        }

        if (!hasEnoughSlots(1, leader))
            return EntryResponse.FAILURE_PER_SLOTS;

        if (leader.getLevel() < getEntrance().playerMinimumLevel())
            return EntryResponse.FAILURE_PER_LEVEL;

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

    public void onEnter(final Player player) {

        if (player == null) return;

        joinPoints.put(player.getUniqueId(), player.getLocation());
        if (isTimed()) {
            System.out.println("Is timed: " + isTimed());
            SessionManager.getInstance().startTimedSession(this,
                    player.getUniqueId(),
                    getParser().getTimeAmount(),
                    getParser().getTimeUnit(),
                    s -> {
                final Player p = Bukkit.getPlayer(s);
                p.sendMessage("ciao");
                onExit(p);
            });

        } else
            SessionManager.getInstance()
                    .startSession(this,
                            player.getUniqueId());

        System.out.println(getEntrance().onEnterCommands());

        CommandUtils.executeMulti(
                Bukkit.getConsoleSender(),
                getEntrance().onEnterCommands(),
                player);

    }

    @Override
    public void onEnter(final Player... players) {
        Arrays.stream(players).forEach(this::onEnter);
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
        leaders.clear();
        parser = null;
    }

    private boolean isTimed() {
        return typeFlags().contains(TypeFlag.TIMED);
    }

    public DungeonParser getParser() {
        return parser;
    }
}
