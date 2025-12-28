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
     *
     * */
    public List<Reward> parse() {

        // Initializing array of rewards
        Reward[] rewards = new Reward[0];

        // Taking from parser the dungeon section about rewards
        final ConfigurationSection section = dungeonConf.getConfigurationSection("rewards");

        // Checking if configuration has this section
        if (section != null) {

            // Taking subsections of rewards so each reward
            final Set<String> subSections = section.getKeys(false);

            // Check if there's some rewards registered
            if (!subSections.isEmpty()) {

                // Initializing rewards to reward final size taken from amount of rewards present in the config
                rewards = new Reward[subSections.size()];

                int i = 0;
                // Iterating along all subsections
                for (final String s : subSections) {

                    final String sub = "rewards." + s;

                    // Initializing items rewards array
                    ItemReward[] itemsR = new ItemReward[0];
                    final ConfigurationSection itemSec = section.getConfigurationSection(s + ".items");
                    // Item parsing

                    // Checking if there's some items in this reward
                    if (itemSec != null) {
                        final Set<String> items = itemSec.getKeys(false);

                        if (!items.isEmpty()) {
                            itemsR = new ItemReward[items.size()];

                            int ia = 0;
                            for (final String item : items) {

                                // Building up the items
                                itemsR[ia++] = new ItemReward() {
                                    @Override
                                    public boolean isMythicItem() {
                                        return itemSec.getBoolean(item + ".is_mythic_item");
                                    }

                                    @Override
                                    public Optional<String> mythicItemName() {
                                        return Optional.ofNullable(itemSec.getString(item + ".mythic_item_name"));
                                    }

                                    @Override
                                    public Optional<String> type() {
                                        return Optional.ofNullable(itemSec.getString(item + ".type"));
                                    }

                                    @Override
                                    public int amount() {
                                        return itemSec.getInt(item + ".amount");
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
                    ItemReward[] finalItemsR = itemsR;

                    // Building the reward with the abstract class
                    AbstractReward reward = new AbstractReward() {

                        private final String id = sub;
                        private final ItemReward[] itemsRewards = finalItemsR;
                        private final double experience = section.getDouble(s + ".experience");
                        private final List<String> commands = section.getStringList(s + ".commands");

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

            }

        }

        return Arrays.stream(rewards).toList();
    }

}
