package com.littlesquad;

import com.littlesquad.dungeon.internal.file.FileManager;
import com.littlesquad.dungeon.internal.utils.MessageProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class Main extends JavaPlugin {
    private static Logger logger;
    static MessageProvider messageProvider;

    @Override
    public void onEnable () {
        logger = getLogger();
        FileManager.loadAll(getDataFolder())
                .thenRunAsync(() -> {

                    //TODO: Register all the services

                });
    }

    @Override
    public void onDisable () {
        FileManager.close();
    }

    public static Logger getDungeonLogger () {
        return logger;
    }
    public static MessageProvider getMessageProvider () {
        return messageProvider;
    }
}
