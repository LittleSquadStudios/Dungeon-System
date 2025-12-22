package com.littlesquad.dungeon.internal.checkpoint;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.checkpoint.AbstractCheckPoint;
import com.littlesquad.dungeon.api.checkpoint.Checkpoint;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Arrays;
import java.util.List;

public final class CheckPointImpl extends AbstractCheckPoint {
    private Checkpoint lazyRespawnCheckpoint;
    private final String respawnCheckpoint;

    public CheckPointImpl (final String id,
                           final Location loc,
                           final String respawnCheckpoint,
                           final List<String> onDeathCommands) {
        super(id, loc);
        this.respawnCheckpoint = respawnCheckpoint;
        this.onDeathCommands.addAll(onDeathCommands);
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        CheckPointManager.checkpoints.compute(id, (_, v) -> {
            if (v != null) {
                Main.getDungeonLogger().warning(Main
                        .getMessageProvider()
                        .getConsolePrefix()
                        + Main
                        .getMessageProvider()
                        .getMessage("config.dungeon.already_existing_checkpoint")
                        + '\''
                        + id
                        + '\'');
                return v;
            }
            return this;
        });
    }

    public void respawnAtCheckpoint (final Player player) {
        player.teleport(getLocation());
    }
    public Checkpoint getRespawnCheckpoint () {
        return lazyRespawnCheckpoint != null
                ? lazyRespawnCheckpoint
                : (lazyRespawnCheckpoint = CheckPointManager.get(respawnCheckpoint));
    }
    public void unlockFor (final Player... players) {
        Arrays.stream(players)
                .parallel()
                .forEach(player -> CheckPointManager
                        .playerCheckpoints
                        .put(player.getUniqueId(), this));
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath (final PlayerDeathEvent e) {
        e.setCancelled(true);
        getRespawnCheckpoint().respawnAtCheckpoint(e.getPlayer());
        onDeathCommands.forEach(command -> Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                PlaceholderFormatter.formatPerPlayer(command, e.getPlayer())));
    }

    public void close () {
        PlayerDeathEvent.getHandlerList().unregister(this);
        CheckPointManager.checkpoints.remove(id);
    }
}
