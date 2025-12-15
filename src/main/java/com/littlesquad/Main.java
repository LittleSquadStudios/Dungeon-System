package com.littlesquad;

import com.littlesquad.dungeon.internal.file.FileManager;
import com.littlesquad.dungeon.internal.utils.MessageProvider;
import net.Indyuce.mmocore.api.MMOCoreAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class Main extends JavaPlugin {
    private static Main instance;
    private static MMOCoreAPI mmoCoreAPI;
    static MessageProvider messageProvider;

    @Override
    public void onEnable () {
        instance = this;
        FileManager.loadAll(getDataFolder())
                .thenRunAsync(() -> {

                    //TODO: Register all the services

                });
    }

    @Override
    public void onDisable () {
        FileManager.close();
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
}
