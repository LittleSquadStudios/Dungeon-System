package com.littlesquad.dungeon.api.status;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.session.DungeonSession;
import com.littlesquad.dungeon.internal.SessionManager;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractStatus implements Status {

    private final boolean isPvp;
    private final Dungeon dungeon;

    private final ConcurrentHashMap<UUID, Integer> playerDeaths; //TODO: Caching
    //private final ConcurrentHashMap<UUID, Integer> playerKills;

    public AbstractStatus(boolean isPvp, final Dungeon dungeon) {
        this.isPvp = isPvp;
        this.dungeon = dungeon;
        playerDeaths = new ConcurrentHashMap<>();

        // Registering events
        Bukkit.getPluginManager()
                .registerEvents(
                        this,
                        Main.getInstance());

    }

    //TODO: Rewrite the Status for better Diagnostics!

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
        System.out.println("Death Debug - 1");
        if (entity instanceof Player player) {
            System.out.println("Death Debug - 2");
            if (isPlayerInDungeon(player.getUniqueId())) {
                System.out.println("Death Debug - 3");
                final DungeonSession session = SessionManager.getInstance().getSession(player.getUniqueId());
                session.addDeath();
            }
            return;
        }
        System.out.println("Death Debug - 1 - 1");
        final Player killer = entity.getKiller();
        System.out.println("Death Debug - 1 - 2");
        if (killer != null && isPlayerInDungeon(killer.getUniqueId())) {
            System.out.println("Death Debug - 1 - 3");
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
    public int playerKills(UUID player) {
        return SessionManager.getInstance()
                .getSession(player)
                .kills();
    }

    @Override
    public int partyKills(AbstractParty party) {
       return party.getOnlineMembers()
               .stream()
               .map(SynchronizedDataHolder::getUniqueId)
               .mapToInt
                       (uniqueId -> SessionManager.getInstance()
                       .getSession(uniqueId)
                       .kills())
               .sum();
    }

    @Override
    public int totalKills() {
        return SessionManager.getInstance()
                .getSessions()
                .stream()
                .mapToInt(DungeonSession::kills)
                .sum();
    }

    @Override
    public int playerDeaths(UUID uuid) {
        return playerDeaths.get(uuid);
    }

    @Override
    public int partyDeaths(AbstractParty party) {
        return party.getOnlineMembers()
                .stream()
                .map(SynchronizedDataHolder::getUniqueId)
                .mapToInt
                        (uniqueId -> SessionManager.getInstance()
                                .getSession(uniqueId)
                                .deaths())
                .sum();
    }

    @Override
    public int totalDeaths() {
        return SessionManager.getInstance()
                .getSessions()
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
    }

}
