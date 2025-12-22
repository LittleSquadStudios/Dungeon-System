package com.littlesquad.dungeon.internal.utils;

import org.bukkit.configuration.file.FileConfiguration;

public final class MessageProvider {
    private final FileConfiguration messageConfig;

    private final String prefix;
    private final String consolePrefix;

    public MessageProvider (final FileConfiguration messageConfig) {
        this.messageConfig = messageConfig;
        prefix = messageConfig.getString("prefix", "");
        consolePrefix = removeColors(prefix);
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
    private static String removeColors (final String s) {
        return s.replaceAll("§[a-fA-F0-9]", "");
    }
}
