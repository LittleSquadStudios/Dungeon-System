package com.littlesquad.dungeon.api.event;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.boss.BossRoom;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.api.event.requirement.RequirementType;
import com.littlesquad.dungeon.api.event.requirement.Requirements;
import com.littlesquad.dungeon.internal.file.RequirementsParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

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
    public final void onItemPickUp (final EntityPickupItemEvent e) {
        if (e.getEntity().getWorld().equals(dungeon.getWorld()))
            //noinspection deprecation
            requirements.updateRequirements(RequirementType.ITEM, e, e
                    .getItem()
                    .getItemStack()
                    .hasItemMeta()
                    && e
                    .getItem()
                    .getItemStack()
                    .getItemMeta()
                    .hasDisplayName()
                    ? e
                    .getItem()
                    .getItemStack()
                    .getItemMeta()
                    .getDisplayName()
                    : e
                    .getItem()
                    .getItemStack()
                    .getType()
                    .name());
    }

    public void close () {
        EntityDeathEvent.getHandlerList().unregister(this);
        PlayerMoveEvent.getHandlerList().unregister(this);
        PlayerInteractEvent.getHandlerList().unregister(this);
        PlayerInteractAtEntityEvent.getHandlerList().unregister(this);
        EntityPickupItemEvent.getHandlerList().unregister(this);
    }
}
