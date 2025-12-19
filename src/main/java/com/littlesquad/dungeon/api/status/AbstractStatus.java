package com.littlesquad.dungeon.api.status;

import com.littlesquad.dungeon.api.session.DungeonSession;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractStatus implements Status {

    private final boolean isPvp;

    public AbstractStatus(boolean isPvp) {
        this.isPvp = isPvp;
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

        final LivingEntity entity = e.getEntity();

        if (entity instanceof Player player) {
            if (isPlayerInDungeon(player.getUniqueId())) {
                DungeonSession session = sessionManager().getSession(player.getUniqueId());
                if (session != null) {
                    session.setDead();
                }
            }
            return;
        }

        final Player killer = entity.getKiller();
        if (killer != null && isPlayerInDungeon(killer.getUniqueId())) {

            sessionManager()
                    .getSession
                            (killer.getUniqueId()).addKill(1);

        }

    }


    @Override
    public int currentPlayers() {
        return sessionManager().getSessions().size();
    }

    @Override //TODO: Cerca tra le session e se trovi null allora non esiste
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
        return sessionManager().getSession(player).kills();
    }

    @Override
    public int partyKills(AbstractParty party) {

        int kills = 0;

        for (PlayerData onlineMember : party.getOnlineMembers()) {
            kills += sessionManager()
                    .getSession(onlineMember
                            .getUniqueId())
                    .kills();
        }

        return kills;

    }

    @Override
    public int totalKills() {

        int kills = 0;

        for (final DungeonSession session : sessionManager().getSessions()) {
            kills += session.kills();
        }

        return kills;

    }

    @Override
    public boolean isPvp() {
        return isPvp;
    }
}
