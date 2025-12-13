package com.littlesquad.dungeon.internal.event;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.Dungeon;
import com.littlesquad.dungeon.api.event.ObjectiveEvent;
import org.bukkit.entity.EntityType;

public final class RequirementParser {
    private RequirementParser () {}

    private static FailingRequirement failingRequirement (final String error) {
        return _ -> {
            Main.getDungeonLogger().warning(error);
            return false;
        };
    }

    //Cast to 'FailingRequirement' the return for checking if the parsing encountered any error...
    public static ObjectiveEvent.Requirement parseRequirement (final Dungeon dungeon,
                                                               final String requirement) {
        final String[] words = requirement.split(" ");
        switch (words.length) {
            case 3:
                switch (words[0]) {
                    case "slay":
                        final int mobNumber;
                        try {
                            mobNumber = Integer.parseInt(words[1]);
                        } catch (final NumberFormatException _) {
                            return failingRequirement();
                        }
                        final EntityType entityType;
                        try {
                            entityType = EntityType.valueOf(words[2]);
                        } catch (final IllegalArgumentException _) {


                            //TODO: Check if it's a MythicMob instead!

                            return players -> {

                            };
                        }
                        return players -> {

                        };
                }
        }
        return failingRequirement(Main
                .getMessageProvider()
                .getPrefix()
                + Main
                .getMessageProvider()
                .getMessage("config.dungeon.requirement_parsing_error")
                + '\''
                + requirement
                + '\''
                + " for "
                + dungeon.id());
    }
}
