package com.littlesquad.dungeon.internal.commands;

import com.littlesquad.Main;
import com.littlesquad.dungeon.internal.DungeonManager;
import com.littlesquad.dungeon.internal.file.FileManager;
import org.bukkit.command.CommandSender;

public final class ReloadCommandHandler {
    private ReloadCommandHandler () {}

    @SuppressWarnings("SameReturnValue")
    static boolean onReload (final CommandSender sender,
                             final String[] args) {
        if (args.length != 1) {
            Main.getMessageProvider().sendErrorInCommand(
                    sender,
                    "reload.wrong_arguments");
            return false;
        }
        Main.getMessageProvider().sendMessageInCommand(
                sender,
                Main.getMessageProvider().getReloadInitialization());
        DungeonManager.getDungeonManager().clear();
        FileManager.loadAll(Main.getInstance().getDataFolder())
                .thenRunAsync(() -> {
                    Main.reloadMessageProvider();
                    DungeonManager.getDungeonManager().initDungeons();
                    Main.getMessageProvider().sendMessageInCommand(
                            sender,
                            Main.getMessageProvider().getSuccessfulReload());
                });
        return false;
    }
}
