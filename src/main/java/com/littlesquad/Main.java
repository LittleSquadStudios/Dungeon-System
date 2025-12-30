package com.littlesquad;

import com.littlesquad.dungeon.database.MySQLConnector;
import com.littlesquad.dungeon.internal.DungeonManager;
import com.littlesquad.dungeon.internal.commands.DungeonCommand;
import com.littlesquad.dungeon.internal.event.TimedEventImpl;
import com.littlesquad.dungeon.internal.file.FileManager;
import com.littlesquad.dungeon.internal.utils.MessageProvider;
import com.littlesquad.dungeon.placeholder.PlaceholderHook;
import net.Indyuce.mmocore.api.MMOCoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class Main extends JavaPlugin {
    private static Main instance;
    private static MMOCoreAPI mmoCoreAPI;
    static MessageProvider messageProvider;
    private static MySQLConnector connector;

    @Override
    public void onEnable () {
        instance = this;

        mmoCoreAPI = new MMOCoreAPI(this);

        connector = new MySQLConnector("s395033_azurecore",
                "127.0.0.1",
                3307,
                "u395033_ZfMHLJMVpW",
                "Fa003O25QaNrHfIRDKNGF4MJ",
                Executors.newCachedThreadPool());

        FileManager.loadAll(getDataFolder())
                .thenRunAsync(() -> {
                    new PlaceholderHook().register();
                    messageProvider = new MessageProvider(FileManager.getMessages());

                    //TODO: Initialize main-config based services!

                    //TODO: Aggiungere debugging nei comandi per capire se il dispatch command schedula sul
                    //      main-thread oppure rimane sul caller thread!!!

                    DungeonManager.getDungeonManager().initDungeons();
                });




        final PluginCommand mainCommand = Bukkit.getPluginCommand("dungeon");
        final DungeonCommand exTabCompleter = new DungeonCommand(DungeonManager.getDungeonManager());

        if (mainCommand != null) {
            mainCommand.setExecutor(exTabCompleter);
            mainCommand.setTabCompleter(exTabCompleter);
        } else System.out.println("Command doesn't exists in plugin.yml");

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
