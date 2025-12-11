package com.littlesquad;

import com.littlesquad.dungeon.internal.file.FileManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable () {
        FileManager.loadAll(getDataFolder())
                .thenAccept(v -> {



                });
    }

    @Override
    public void onDisable () {
        FileManager.close();
    }

}
