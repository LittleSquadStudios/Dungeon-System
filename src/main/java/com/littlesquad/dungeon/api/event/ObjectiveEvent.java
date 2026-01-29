package com.littlesquad.dungeon.api.event;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.event.requirement.RequirementType;
import com.littlesquad.dungeon.api.event.requirement.Requirements;
import com.littlesquad.dungeon.internal.file.RequirementsParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public abstract non-sealed class ObjectiveEvent implements Event {
    private final Dungeon dungeon;
    private final String id;

    private final Requirements requirements;

    protected ObjectiveEvent (final Dungeon dungeon,
                              final String id,
                              final RequirementsParser parser) {
        this.dungeon = dungeon;
        this.id = id;
        requirements = parser.parse(this);
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public final EventType getType () {
        return EventType.OBJECTIVE;
    }

    public Dungeon getDungeon () {
        return dungeon;
    }
    public String getID () {
        return id;
    }

    public abstract Checkpoint checkpointToUnlock ();
    public abstract BossRoom bossRoomToUnlock ();

    //Done like this for giving the possibility to handle additional
    //operations like deactivating the event for the player/party!
    public abstract void executeCommandsFor (final Player... players);
    public abstract void deActiveFor (final Player... players);

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onEntityDeath (final EntityDeathEvent e) {
        if (e.getEntity().getWorld().equals(dungeon.getWorld()))
            requirements.updateRequirements(RequirementType.SLAY, e);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onPlayerMove (final PlayerMoveEvent e) {
        if (e.getPlayer().getWorld().equals(dungeon.getWorld()))
            requirements.updateRequirements(RequirementType.MOVE, e);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onPlayerInteract (final PlayerInteractEvent e) {
        if (e.getPlayer().getWorld().equals(dungeon.getWorld()))
            requirements.updateRequirements(RequirementType.INTERACT, e);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onPlayerInteractAtEntity (final PlayerInteractAtEntityEvent e) {
        if (e.getPlayer().getWorld().equals(dungeon.getWorld()))
            requirements.updateRequirements(RequirementType.INTERACT, e);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onItemPickUp (final InventoryEvent e) {
        final AtomicReference<HumanEntity> humanEntity = new AtomicReference<>();
        final Inventory inventory;
        if (Objects.equals((inventory = e
                .getInventory())
                .getViewers()
                .stream()
                .filter(he -> {
                    if (he.getInventory().equals(inventory)) {
                        humanEntity.setPlain(he);
                        return true;
                    } else return false;
                }).map(HumanEntity::getWorld)
                .findAny()
                .orElse(null), dungeon.getWorld()))
            requirements.updateRequirements(RequirementType.ITEM, e, humanEntity.getPlain());
    }

    public void close () {
        EntityDeathEvent.getHandlerList().unregister(this);
        PlayerMoveEvent.getHandlerList().unregister(this);
        PlayerInteractEvent.getHandlerList().unregister(this);
        PlayerInteractAtEntityEvent.getHandlerList().unregister(this);
        InventoryEvent.getHandlerList().unregister(this);
    }
}
