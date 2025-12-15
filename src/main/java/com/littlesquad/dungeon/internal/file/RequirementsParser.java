package com.littlesquad.dungeon.internal.file;

import com.littlesquad.Main;
import com.littlesquad.dungeon.api.event.ObjectiveEvent;
import com.littlesquad.dungeon.api.event.requirement.Requirements;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RequirementsParser {
    private final FileConfiguration config;
    private final ExecutorService ex = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());

    RequirementsParser (final FileConfiguration config) {
        this.config = config;
    }

    //Cast to 'FailingRequirement' the return for checking if the parsing encountered any error...
    public Requirements parse (final ObjectiveEvent event) {

        //TODO: Remember to check if the event 'isActiveFor()' the players!

        final String[] requirements;

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

    public void close () {
        ex.shutdownNow();
    }
}
