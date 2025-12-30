package com.littlesquad.dungeon.internal.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public final class MessageProvider {
    private final FileConfiguration messageConfig;

    private final String prefix;
    private final String consolePrefix;

    private final String enqueuedForBossRoom;
    private final String eventTriggeredForParty;
    private final String eventTriggeredForPlayer;
    private final String eventDeactivated;

    public MessageProvider (final FileConfiguration messageConfig) {
        this.messageConfig = messageConfig;
        prefix = messageConfig
                .getString("prefix", "")
                .replaceAll("&", "§");
        consolePrefix = removeColors(prefix);
        enqueuedForBossRoom = messageConfig
                .getString("boss.enqueued", "")
                .replaceAll("&", "§");
        eventTriggeredForParty = messageConfig
                .getString("event.event_triggered_for_party", "")
                .replaceAll("&", "§");
        eventTriggeredForPlayer = messageConfig
                .getString("event.event_triggered_for_player", "")
                .replaceAll("&", "§");
        eventDeactivated = messageConfig
                .getString("event.event_deactivated", "")
                .replaceAll("&", "§");
    }

    //Cache only the important messages (don't waste memory and fields on errors that shouldn't happen)!
    public String getMessage (final String configPath) {
        return messageConfig.getString(configPath, "");
    }

    //For cached messages or strings use custom methods!
    public String getPrefix () {
        return prefix;
    }
    public String getConsolePrefix () {
        return consolePrefix;
    }
    public static String removeColors (final String s) {
        return s.replaceAll("§|&[a-fA-F0-9]", "");
    }

    public void sendErrorInCommand (final CommandSender sender,
                                    final String path) {
        if (sender instanceof Player)
            sender.sendMessage(getPrefix() + getMessage(path));
        else sender.sendMessage(getConsolePrefix() + MessageProvider.removeColors(getMessage(path)));
    }

    public void sendMessageInCommand (final CommandSender sender,
                                      final String message) {
        if (message.isEmpty())
            return;
        if (sender instanceof Player)
            sender.sendMessage(getPrefix() + message);
        else sender.sendMessage(getConsolePrefix() + MessageProvider.removeColors(message));
    }

    public String getEnqueuedForBossRoom () {
        return enqueuedForBossRoom;
    }
    public String getEventTriggeredForParty () {
        return eventTriggeredForParty;
    }
    public String getEventTriggeredForPlayer () {
        return eventTriggeredForPlayer;
    }
    public String getEventDeactivated () {
        return eventDeactivated;
    }
}
