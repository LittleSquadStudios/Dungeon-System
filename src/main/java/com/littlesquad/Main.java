package com.littlesquad;

import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.database.MySQLConnector;
import com.littlesquad.dungeon.internal.DungeonManager;
import com.littlesquad.dungeon.internal.SessionManager;
import com.littlesquad.dungeon.internal.commands.DungeonCommand;
import com.littlesquad.dungeon.internal.file.ConfigParser;
import com.littlesquad.dungeon.internal.file.FileManager;
import com.littlesquad.dungeon.internal.utils.MessageProvider;
import com.littlesquad.dungeon.placeholder.PlaceholderHook;
import net.Indyuce.mmocore.api.MMOCoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class Main extends JavaPlugin {
    private static final ExecutorService CACHED = Executors.newCachedThreadPool();
    private static final ExecutorService WORK_STEALING = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
    private static final ScheduledExecutorService SCHEDULED = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    private static Main instance;
    private static MMOCoreAPI mmoCoreAPI;
    static MessageProvider messageProvider;
    private static MySQLConnector connector;

    @Override
    public void onEnable () {
        instance = this;

        mmoCoreAPI = new MMOCoreAPI(this);

        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                || !new PlaceholderHook().register())
            getDungeonLogger().warning("PlaceholderAPI not Hooked");
        FileManager.loadAll(getDataFolder())
                .thenRunAsync(() -> {
                    reloadMessageProvider();
                    connector = FileManager.getConfig().createConnector();
                    if (connector != null)
                        DungeonManager.getDungeonManager().initDungeons();
                    else Bukkit.getPluginManager().disablePlugin(this);
                });

        final PluginCommand mainCommand = Bukkit.getPluginCommand("dungeon");
        final DungeonCommand exTabCompleter = new DungeonCommand(DungeonManager.getDungeonManager());

        if (mainCommand != null) {
            mainCommand.setExecutor(exTabCompleter);
            mainCommand.setTabCompleter(exTabCompleter);
        } else System.out.println("Command doesn't exists in plugin.yml");

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onDisable () {
        SessionManager.getInstance().shutdown();
        DungeonManager
                .getDungeonManager()
                .getAllDungeons()
                .forEach(Dungeon::shutdown);
        CACHED.shutdown();
        WORK_STEALING.shutdown();
        SCHEDULED.shutdown();
        try {
            CACHED.awaitTermination(60L, TimeUnit.SECONDS);
        } catch (final InterruptedException _) {}
        try {
            WORK_STEALING.awaitTermination(60L, TimeUnit.SECONDS);
        } catch (final InterruptedException _) {}
        try {
            SCHEDULED.awaitTermination(60L, TimeUnit.SECONDS);
        } catch (final InterruptedException _) {}
    }

    public static Main getInstance () {
        return instance;
    }

    public static MMOCoreAPI getMMOCoreAPI () {
        return mmoCoreAPI;
    }

    public static Logger getDungeonLogger () {
        return instance.getLogger();
    }

    public static MessageProvider getMessageProvider () {
        return messageProvider;
    }
    public static void reloadMessageProvider () {
        messageProvider = new MessageProvider(FileManager.getMessages());
    }

    public static MySQLConnector getConnector() {
        return connector;
    }


    public static ExecutorService getCachedExecutor () {
        return CACHED;
    }
    public static ExecutorService getWorkStealingExecutor () {
        return WORK_STEALING;
    }
    public static ScheduledExecutorService getScheduledExecutor () {
        return SCHEDULED;
    }
}
