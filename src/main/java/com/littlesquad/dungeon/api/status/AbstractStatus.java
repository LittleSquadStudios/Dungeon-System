package com.littlesquad.dungeon.api.status;

import com.littlesquad.dungeon.api.session.DungeonSession;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractStatus implements Status {

    private final boolean isPvp;

    private final ConcurrentHashMap<UUID, Integer> playerDeaths;

    public AbstractStatus(boolean isPvp) {
        this.isPvp = isPvp;
        playerDeaths = new ConcurrentHashMap<>();
    }

    @EventHandler
    public void onPvp(final EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Player damager) || !(e.getEntity() instanceof Player damaged)) {
            return;
        }

        if (isPlayerInDungeon
                (damager.getUniqueId()) &&
                isPlayerInDungeon
                        (damaged.getUniqueId())) {

            if (isPvp) {

                sessionManager()
                        .getSession(damager.getUniqueId())
                        .addDamage(e.getDamage());

                sessionManager()
                        .getSession(damaged.getUniqueId())
                        .addDamageTaken(damaged.getLastDamage());

            } else e.setCancelled(true);

        }

    }

    @EventHandler
    public void onPve(final EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Player damager)) {
            return;
        }

        if (isPlayerInDungeon
                (damager.getUniqueId())) {

            if (e.getEntity() instanceof LivingEntity) {

                sessionManager()
                        .getSession
                                (damager.getUniqueId())
                        .addDamage
                                (e.getFinalDamage());

            }

        }

    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent e) {
        // TODO: Stop session if player dies
        final LivingEntity entity = e.getEntity();

        if (entity instanceof Player player) {
            if (isPlayerInDungeon(player.getUniqueId())) {

                final DungeonSession session = sessionManager().getSession(player.getUniqueId());


                session.setDead();
                session.stopSession();
            }
            return;
        }

        final Player killer = entity.getKiller();

        if (killer != null && isPlayerInDungeon(killer.getUniqueId()))
            sessionManager()
                    .getSession(killer.getUniqueId())
                    .addKill(1);



    }


    @Override
    public int currentPlayers() {
        return sessionManager().getSessions().size();
    }

    @Override
    public boolean isPlayerInDungeon(UUID player) {
        return sessionManager().getSession(player) != null;
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
        return sessionManager()
                .getSession(player)
                .kills();
    }

    @Override
    public int partyKills(AbstractParty party) {
       return party.getOnlineMembers()
               .stream()
               .map(SynchronizedDataHolder::getUniqueId)
               .mapToInt
                       (uniqueId -> sessionManager()
                       .getSession(uniqueId)
                       .kills())
               .sum();
    }

    @Override
    public int totalKills() {
        return sessionManager()
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
    public boolean isPvp() {
        return isPvp;
    }
}
