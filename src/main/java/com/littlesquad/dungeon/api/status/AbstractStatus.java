package com.littlesquad.dungeon.api.status;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.entrance.ExitReason;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.internal.SessionManager;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractStatus implements Status {

    private final boolean isPvp;
    private final Dungeon dungeon;
    private final ConcurrentHashMap<UUID, ExitReason> exitReasons = new ConcurrentHashMap<>();

    public AbstractStatus(boolean isPvp, final Dungeon dungeon) {
        this.isPvp = isPvp;
        this.dungeon = dungeon;

        Bukkit.getPluginManager()
                .registerEvents(
                        this,
                        Main.getInstance());

    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent e) {
        if (isPlayerInDungeon(e.getPlayer().getUniqueId())) {
            switch (e.getReason()) {
                case TIMED_OUT, ERRONEOUS_STATE -> { // CASE TEST
                    final DungeonSession session = SessionManager.getInstance().getSession(e.getPlayer().getUniqueId());
                    if (session != null)
                        session.stopSession(ExitReason.ERROR);
                    exitReasons.put(e.getPlayer().getUniqueId(), ExitReason.ERROR);
                }
                default -> exitReasons.put(e.getPlayer().getUniqueId(), ExitReason.QUIT);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(final AsyncPlayerPreLoginEvent event) {

        final UUID uuid = event.getUniqueId();
        final ExitReason reason = exitReasons.get(uuid);

        if (reason != null)
            if(reason.equals(ExitReason.ERROR)) {
                SessionManager.getInstance().recoverActiveSessions(uuid, _ -> {});
            }

    }

    @EventHandler
    public void handleDamageEvent(final EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof final Player damager
                && e.getEntity() instanceof final Player damaged) {
            if (isPlayerInDungeon
                    (damager.getUniqueId()) &&
                    isPlayerInDungeon(damaged.getUniqueId())) {
                if (isPvp) {
                    final DungeonSession damagerSession = SessionManager
                            .getInstance()
                            .getSession(damager.getUniqueId());
                    final DungeonSession damagedSession = SessionManager
                            .getInstance()
                            .getSession(damaged.getUniqueId());

                    if (damagerSession != null && damagedSession != null) {
                        damagerSession.addDamage(e.getFinalDamage());
                        damagedSession.addDamageTaken(e.getFinalDamage());
                    }

                } else e.setCancelled(true);
            }
        } else if (e.getDamager() instanceof Player damager){
            if (isPlayerInDungeon
                    (damager.getUniqueId())) {
                if (e.getEntity() instanceof LivingEntity) {
                    SessionManager.getInstance()
                            .getSession(damager.getUniqueId())
                            .addDamage(e.getFinalDamage());
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent e) {
        final LivingEntity entity = e.getEntity();
        if (entity instanceof Player player) {
            if (isPlayerInDungeon(player.getUniqueId())) {
                final DungeonSession session = SessionManager.getInstance().getSession(player.getUniqueId());
                session.addDeath();
            }
            return;
        }
        final Player killer = entity.getKiller();
        if (killer != null && isPlayerInDungeon(killer.getUniqueId())) {
            SessionManager
                    .getInstance()
                    .getSession(killer.getUniqueId())
                    .addKill(1);
        }
    }


    @Override
    public int currentPlayers() {
        return SessionManager
                .getInstance()
                .getDungeonSessions(dungeon)
                .size();
    }

    @Override
    public boolean isPlayerInDungeon(UUID player) {
        final DungeonSession session;
        return (session = SessionManager
                .getInstance()
                .getSession(player))
                != null
                && session.getDungeon()
                .id()
                .equals(dungeon.id());
    }

    @Override
    public boolean isPartyInDungeon(AbstractParty party) {
        for (final PlayerData pd : party.getOnlineMembers())
            if (!isPlayerInDungeon(pd.getUniqueId()))
                return false;
        return true;
    }


    /*@Override //TODO: Stessa cosa di sopra se quello vale per tutti i membri del party
    public CompletableFuture<Boolean> isPartyInDungeon(AbstractParty party) {
        return CompletableFuture.supplyAsync(() -> {

            @SuppressWarnings("all")
            final class AtomicBoolean {
                private static final VarHandle valueVar;

                static {
                    try {
                        valueVar = MethodHandles.lookup().findVarHandle(AtomicBoolean.class, "value", boolean.class);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                private boolean value;

                private AtomicBoolean(final boolean value) {
                    this.value = value;
                }

            }

            final AtomicBoolean result = new AtomicBoolean(true);
            int partySize = party.getOnlineMembers().size();

            //noinspection unchecked
            final CompletableFuture<Void>[] futures = new CompletableFuture[partySize];

            int i = 0;

            for (final PlayerData pd : party.getOnlineMembers()) {

                futures[i] = isPlayerInDungeon(pd.getUniqueId())
                        .thenAcceptAsync(s ->
                                AtomicBoolean.valueVar.getAndBitwiseAnd(result, s));

                i++;

            }

            CompletableFuture.allOf(futures).join();

            return (boolean) AtomicBoolean.valueVar.getOpaque(result);
        });
    }*/
    @Override
    public int playerKills(UUID uuid) {
        final DungeonSession session = SessionManager
                .getInstance()
                .getSession(uuid);
        if (session != null
                && session
                .getDungeon()
                .id()
                .equals(dungeon.id()))
            return session.kills();
        else return -1;
    }

    @Override
    public int playerDeaths(UUID uuid) {
        final DungeonSession session = SessionManager
                .getInstance()
                .getSession(uuid);
        if (session != null
                && session
                .getDungeon()
                .id()
                .equals(dungeon.id()))
            return session.deaths();
        else return -1;
    }

    @Override
    public int partyKills(AbstractParty party) {
        return party.getOnlineMembers()
                .stream()
                .map(SynchronizedDataHolder::getUniqueId)
                .mapToInt
                        (this::playerKills)
                .sum();
    }

    @Override
    public int partyDeaths(AbstractParty party) {
        return party.getOnlineMembers()
                .stream()
                .map(SynchronizedDataHolder::getUniqueId)
                .mapToInt
                        (this::playerDeaths)
                .sum();
    }

    @Override
    public int totalKills() {
        return SessionManager
                .getInstance()
                .getDungeonSessions(dungeon)
                .stream()
                .mapToInt(DungeonSession::kills)
                .sum();
    }

    @Override
    public int totalDeaths() {
        return SessionManager
                .getInstance()
                .getDungeonSessions(dungeon)
                .stream()
                .mapToInt(DungeonSession::deaths)
                .sum();
    }

    @Override
    public boolean isPvp() {
        return isPvp;
    }

    @Override
    public void shutdown() {
        EntityDeathEvent
                .getHandlerList()
                .unregister(this);
        EntityDamageByEntityEvent
                .getHandlerList()
                .unregister(this);
        AsyncPlayerPreLoginEvent
                .getHandlerList()
                .unregister(this);
        PlayerQuitEvent
                .getHandlerList()
                .unregister(this);
    }

}
