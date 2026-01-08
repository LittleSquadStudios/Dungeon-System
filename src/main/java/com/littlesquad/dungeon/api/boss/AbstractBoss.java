package com.littlesquad.dungeon.api.boss;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.rewards.AbstractReward;
import com.littlesquad.dungeon.api.rewards.Reward;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.api.mobs.entities.SpawnReason;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBoss implements Boss, Listener {

    private final BossRoom room;
    private MythicMob bossEntity;
    private ActiveMob activeMob;
    private volatile BossState state;
    private final Set<UUID> participants;
    private UUID bossUUID;

    public AbstractBoss(final BossRoom room) {
        this.room = room;
        this.state = BossState.NOT_SPAWNED;
        this.participants = ConcurrentHashMap.newKeySet();

        // Register this as a listener
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @Override
    public void spawn() {
        if (state != BossState.NOT_SPAWNED && state != BossState.DEAD) {
            return;
        }

        state = BossState.SPAWNING;

        try {
            // Retrieve the MythicMob definition
            final Optional<MythicMob> mythicMobOptional = MythicProvider.get()
                    .getMobManager()
                    .getMythicMob(bossName());

            if (mythicMobOptional.isEmpty()) {
                state = BossState.NOT_SPAWNED;
                System.err.println("[BossSpawn] Boss mob type '" + bossName() + "' not found");
                return;
            }

            bossEntity = mythicMobOptional.get();

            // Calculate the boss level
            final double calculatedLevel = calculateBossLevel(Arrays.stream(room
                    .getPlayersIn())
                    .parallel()
                    .mapToInt(p -> Main
                            .getMMOCoreAPI()
                            .getPlayerData(p)
                            .getLevel())
                    .sum());

            // Get spawn location
            final Location spawnLocation = getSpawnLocation();
            if (spawnLocation == null) {
                state = BossState.NOT_SPAWNED;
                System.err.println("[BossSpawn] Invalid spawn location for boss room");
                return;
            }

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                final ActiveMob spawnedMob = bossEntity.spawn(
                        BukkitAdapter.adapt(spawnLocation),
                        calculatedLevel,
                        SpawnReason.COMMAND);

                if (spawnedMob != null) {
                    this.activeMob = spawnedMob;
                    this.bossUUID = spawnedMob.getEntity().getUniqueId();
                    state = BossState.ALIVE;
                    participants.clear();
                    onSpawn();
                } else {
                    state = BossState.NOT_SPAWNED;
                    System.err.println("[BossSpawn] Failed to spawn boss after 1 attempt");
                }
            });

        } catch (final Exception e) {
            state = BossState.NOT_SPAWNED;
            System.err.println("[BossSpawn] Exception during boss spawn: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBossDeath(final MythicMobDeathEvent event) {
        if (!isBossEvent(event.getMobType().getInternalName(), event.getEntity().getUniqueId()))
            return;
        Main.getWorkStealingExecutor().execute(() -> {
            final List<Reward> rewards = room.rewards();
            if (rewards == null || rewards.isEmpty())
                for (final UUID participantId : participants) {
                    final Player player = Bukkit.getPlayer(participantId);
                    if (player != null && player.isOnline()) {
                        assert rewards != null;
                        for (final Reward reward : rewards)
                            ((AbstractReward) reward).give(player);
                    }
                }
            state = BossState.DEAD;
            onDeath();
            final Player[] players = room.getPlayersIn();

            //TODO: set a timer for taking the rewards, then expel from the dungeon

            //Example
            room.kick(() -> room.getDungeon().onExit(players), players);

            //TODO: Map the players to their current boss-room and before closing the session, try to kick it!
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBossDespawn(final MythicMobDespawnEvent event) {
        if (!isBossEvent(event.getMobType().getInternalName(), event.getEntity().getUniqueId())) {
            return;
        }

        if (state == BossState.ALIVE) {
            //No reason to despawn the mob since this method is signaling that it has been despawned!
            state = BossState.DESPAWNED;
            onDespawn();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamageBoss(final EntityDamageByEntityEvent event) {
        if (state != BossState.ALIVE || bossUUID == null) {
            return;
        }

        // Check if the damaged entity is our boss
        if (!event.getEntity().getUniqueId().equals(bossUUID)) {
            return;
        }

        // Check if damage source is a player
        Player damager = null;
        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } /*else if (event.getDamager() instanceof org.bukkit.projectiles.Projectile) {
            org.bukkit.projectiles.Projectile projectile = (org.bukkit.projectiles.Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                damager = (Player) projectile.getShooter();
            }
        }*/

        if (damager != null) {
            participants.add(damager.getUniqueId());
        }
    }

    public void close () {
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        MythicMobDespawnEvent.getHandlerList().unregister(this);
        MythicMobDeathEvent.getHandlerList().unregister(this);
    }

    private boolean isBossEvent(String mobTypeName, UUID entityUUID) {
        return mobTypeName.equals(bossName()) &&
                bossUUID != null &&
                bossUUID.equals(entityUUID);
    }

    @Override
    public BossState getState() {
        return state;
    }

    @Override
    public boolean isAlive() {
        if (state != BossState.ALIVE || activeMob == null) {
            return false;
        }

        try {
            return !activeMob.isDead() && activeMob.getEntity().getBukkitEntity() instanceof LivingEntity
                    && ((LivingEntity) activeMob.getEntity().getBukkitEntity()).getHealth() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<MythicMob> getSpawnedEntity() {
        return Optional.ofNullable(bossEntity);
    }

    @Override
    public Set<UUID> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    public Optional<ActiveMob> getActiveMob() {
        return Optional.ofNullable(activeMob);
    }

    /**
     * Returns the reference party level for scaling.
     * @return party level
     */
    public abstract int partyLevel();

    /**
     * Returns the multiplier for the boss level calculation.
     * @return multiplier
     */
    public abstract int multiplier();

    /**
     * Returns the exponent for non-linear scaling in boss level calculation.
     * @return exponent
     */
    public abstract int exponent();

    /**
     * Returns the maximum level the boss can reach.
     * @return max level
     */
    public abstract int maxLevel();

    /**
     * Calculates the level of the boss based on party information
     * and configuration parameters.
     *
     * @param actualPartyLevel the sum of levels of all party members
     * @return the calculated boss level
     */
    public double calculateBossLevel(final double actualPartyLevel) {
        return Math.max(
                maxLevel(),
                Math.min(
                        baseLevel(),
                        Math.pow(
                                (baseLevel() + (actualPartyLevel - partyLevel())) * multiplier(),
                                exponent()
                        )
                )
        );
    }
}