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

public final class RewardParser {

    private final FileConfiguration dungeonConf;

    RewardParser(final FileConfiguration dungeonConf) {
        this.dungeonConf = dungeonConf;
    }

    public List<Reward> parse() {


        Reward[] rewards = new Reward[0];

        final ConfigurationSection section = dungeonConf.getConfigurationSection("rewards");

        if (section != null) {

            final Set<String> subSections = section.getKeys(false);

            if (!subSections.isEmpty()) {
                rewards = new Reward[subSections.size()];

                int i = 0;
                for (final String sub : subSections) {

                    ItemReward[] itemsR = new ItemReward[0];
                    final ConfigurationSection itemSec = section.getConfigurationSection(sub + ".items");
                    // Item parsing

                    if (itemSec != null) {
                        final Set<String> items = itemSec.getKeys(false);

                        if (!items.isEmpty()) {
                            itemsR = new ItemReward[items.size()];

                            int ia = 0;
                            for (final String item : items) {

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


                    ItemReward[] finalItemsR = itemsR;
                    rewards[i] = new AbstractReward() {

                        private final String id = sub;
                        private final ItemReward[] itemsRewards = finalItemsR;
                        private final double experience = section.getDouble(sub + ".experience");
                        private final List<String> commands = section.getStringList(sub + ".commands");

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

                    i++;
                }

            }

        }

        return Arrays.stream(rewards).toList();
    }

}
