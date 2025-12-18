package com.littlesquad.dungeon.internal.utils;

public final class MessageProvider {

    public MessageProvider () {}

    //Cache only the important messages (don't waste memory and fields on errors that shouldn't happen)!
    public String getMessage (final String configPath) {
        return null;
    }

    //For cached messages or strings use custom methods!
    public String getPrefix () {
        return null;
    }
    public String getConsolePrefix () {
        return null;
    }
    private static String removeColors (final String s) {
        return s.replaceAll("§[a-fA-F0-9]", "");
    }
}
