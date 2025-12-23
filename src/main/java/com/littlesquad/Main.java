package com.littlesquad;

import com.littlesquad.dungeon.database.MySQLConnector;
import com.littlesquad.dungeon.internal.DungeonManager;
import com.littlesquad.dungeon.internal.event.TimedEventImpl;
import com.littlesquad.dungeon.internal.file.FileManager;
import com.littlesquad.dungeon.internal.utils.MessageProvider;
import com.littlesquad.dungeon.placeholder.PlaceholderHook;
import net.Indyuce.mmocore.api.MMOCoreAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Logger;

public final class Main extends JavaPlugin {
    private static Main instance;
    private static MMOCoreAPI mmoCoreAPI;
    static MessageProvider messageProvider;
    private static MySQLConnector connector;

    @Override
    public void onEnable () {
        instance = this;
        FileManager.loadAll(getDataFolder())
                .thenRunAsync(() -> {

                    //TODO: Register all the services
                    DungeonManager.getDungeonManager().initDungeons();

                    new PlaceholderHook().register();
                });

        connector = new MySQLConnector("dungeon-system-db",
                "127.0.0.1",
                3306,
                "root",
                "",
                Executors.newCachedThreadPool());

    }

    @Override
    public void onDisable () {
        FileManager.close();
        TimedEventImpl.close();
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

    public static MySQLConnector getConnector() {
        return connector;
    }

}
