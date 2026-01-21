package com.littlesquad.dungeon.internal.file;

import com.littlesquad.dungeon.api.rewards.AbstractReward;
import com.littlesquad.dungeon.api.rewards.ItemReward;
import com.littlesquad.dungeon.api.rewards.Reward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@SuppressWarnings("ClassCanBeRecord")
public final class RewardParser {

    private final FileConfiguration dungeonConf;

    RewardParser(final FileConfiguration dungeonConf) {
        this.dungeonConf = dungeonConf;
    }

    /**
     * This method is used to parse a list of rewards starting from a dungeon config
     * he takes first the items then build them up and cache it all
     *
     * @since 1.0.0
     * @author LittleSquad
     */
    public List<Reward> parse() {
        // Initializing array of rewards
        final Reward[] rewards = new Reward[0];

        // Taking from parser the dungeon section about rewards
        final ConfigurationSection section = dungeonConf.getConfigurationSection("rewards");

        // Checking if configuration has this section
        if (section == null) {
            return Arrays.stream(rewards).toList();
        }

        // Taking subsections of rewards so each reward
        final Set<String> subSections = section.getKeys(false);

        // Check if there's some rewards registered
        if (subSections.isEmpty()) {
            return Arrays.stream(rewards).toList();
        }

        // Initializing rewards to reward final size taken from amount of rewards present in the config
        rewards = new Reward[subSections.size()];

        int i = 0;
        // Iterating along all subsections
        for (final String s : subSections) {

            // Verifica esistenza sottosezione
            final ConfigurationSection rewardSection = section.getConfigurationSection(s);
            if (rewardSection == null) {
                continue;
            }

            // Log delle chiavi nella sezione reward

            // Initializing items rewards array
            ItemReward[] itemsR = new ItemReward[0];
            final ConfigurationSection itemSec = section.getConfigurationSection(s + ".items");

            // Checking if there's some items in this reward
            if (itemSec != null) {
                final Set<String> items = itemSec.getKeys(false);

                if (!items.isEmpty()) {
                    itemsR = new ItemReward[items.size()];

                    int ia = 0;
                    for (final String item : items) {

                        String itemPath = item + ".";

                        // Critic fields validation
                        String type = itemSec.getString(itemPath + "type");
                        if (type == null || type.isEmpty()) {
                            continue;
                        }

                        boolean isMythic = itemSec.getBoolean(itemPath + "is_mythic_item");
                        String mythicName = itemSec.getString(itemPath + "mythic_item_name");

                        if (isMythic && (mythicName == null || mythicName.isEmpty())) {
                            continue;
                        }

                        int amount = itemSec.getInt(itemPath + "amount");
                        if (amount < 0) {
                            continue;
                        }

                        // Building up the items
                        itemsR[ia++] = new ItemReward() {
                            @Override
                            public boolean isMythicItem() {
                                return isMythic;
                            }

                            @Override
                            public Optional<String> mythicItemName() {
                                return Optional.ofNullable(mythicName);
                            }

                            @Override
                            public Optional<String> type() {
                                return Optional.of(type);
                            }

                            @Override
                            public int amount() {
                                return amount;
                            }

                            @Override
                            public boolean isGlowing() {
                                return itemSec.getBoolean(item + ".is_glowing");
                            }

                            @Override
                            public List<String> enchantments() {
                                return itemSec.getStringList(item + ".enchants");
                            }

                            @Override
                            public String displayName() {
                                return itemSec.getString(item + ".display_name");
                            }

                            @Override
                            public List<String> lore() {
                                return itemSec.getStringList(item + ".lore");
                            }
                        };
                    }
                }
            }

            // Putting items into a copy array
            final ItemReward[] finalItemsR = itemsR;

            double exp = section.getDouble(s + ".experience");
            List<String> cmds = section.getStringList(s + ".commands"); // IT MAY CAUSE AN ERROR

            // Validazione
            if (section.getDouble(s + ".experience") < 0) {
                continue;
            }

            if (section.getStringList(s + ".commands").isEmpty()) {
                continue;
            }

            AbstractReward reward = new AbstractReward() {

                private final String id = s;
                private final ItemReward[] itemsRewards = finalItemsR;
                private final double experience = exp;
                private final List<String> commands = cmds;

                @Override
                public String id() {
                    return id;
                }

                @Override
                public List<ItemReward> rewards() {
                    return Arrays.stream(itemsRewards).toList();
                }

                @Override
                public double experience() {
                    return experience;
                }

                @Override
                public List<String> commands() {
                    return commands;
                }
            };

            rewards[i] = reward;
            i++;
        }

        return Arrays.stream(rewards).toList();
    }
}