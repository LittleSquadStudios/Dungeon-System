package com.littlesquad.dungeon.api.checkpoint;

import com.littlesquad.Main;
import com.littlesquad.dungeon.internal.checkpoint.CheckPointManager;
import com.littlesquad.dungeon.internal.utils.CommandUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractCheckPoint implements Checkpoint {
    protected final String id;
    protected final Location checkPointLoc;
    protected Checkpoint lazyRespawnCheckpoint;
    protected final String respawnCheckpoint;
    protected final List<String> onDeathCommands;

    protected AbstractCheckPoint (final String id,
                               final Location loc,
                               final String respawnCheckpoint,
                               final List<String> onDeathCommands) {
        this.id = id;
        checkPointLoc = loc;
        this.respawnCheckpoint = respawnCheckpoint;
        this.onDeathCommands = onDeathCommands;
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public String getID() {
        return id;
    }

    public List<String> onDeathCommands() {
        return onDeathCommands;
    }

    public Location getLocation() {
        return checkPointLoc;
    }

    public void unlockFor (final Player... players) {
        Arrays.stream(players)
                .parallel()
                .forEach(player -> CheckPointManager
                        .setCheckPointFor(player, this));
    }

    public void respawnAtCheckpoint (final Player player) {
        player.teleport(getLocation());
    }
    public Checkpoint getRespawnCheckpoint () {
        return lazyRespawnCheckpoint != null
                ? lazyRespawnCheckpoint
                : (lazyRespawnCheckpoint = CheckPointManager.get(respawnCheckpoint));
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.LOW)
    public final void onPlayerDeath (final PlayerDeathEvent e) {
        final Player player = e.getEntity();
        if (CheckPointManager.get(player.getUniqueId()) != this)
            return;
        e.setCancelled(true);
        getRespawnCheckpoint().respawnAtCheckpoint(e.getPlayer());
        CommandUtils.executeMulti(
                Bukkit.getConsoleSender(),
                onDeathCommands,
                e.getPlayer());
    }

    public void close () {
        PlayerDeathEvent.getHandlerList().unregister(this);
        CheckPointManager.unregister(id);
    }
}
