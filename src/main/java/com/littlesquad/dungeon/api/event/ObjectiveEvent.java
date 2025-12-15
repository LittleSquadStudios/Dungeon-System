package com.littlesquad.dungeon.api.event;

import com.littlesquad.Main;
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
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public abstract non-sealed class ObjectiveEvent implements Event {
    private final Requirements requirements;

    protected ObjectiveEvent (final RequirementsParser parser) {
        requirements = parser.parse(this);
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public abstract Checkpoint checkpointToUnlock ();
    public abstract BossRoom bossRoomToUnlock ();

    //Done like this for giving the possibility to handle additional
    //operations like deactivating the event for the player/party!
    public abstract void executeCommandsFor (final Player... players);

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onEntityDeath (final EntityDeathEvent e) {
        requirements.check(RequirementType.SLAY, e);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove (final PlayerMoveEvent e) {
        requirements.check(RequirementType.MOVE, e);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickUp (final InventoryPickupItemEvent e) {
        requirements.check(RequirementType.ITEM, e);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract (final PlayerInteractEvent e) {
        requirements.check(RequirementType.INTERACT, e);
    }
}
