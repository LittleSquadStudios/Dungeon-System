package com.littlesquad.dungeon.internal.file;

import com.littlesquad.dungeon.api.rewards.AbstractReward;
import com.littlesquad.dungeon.api.rewards.ItemReward;
import com.littlesquad.dungeon.api.rewards.Reward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
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
     */
    public List<Reward> parse() {
        // Use ArrayList to avoid null entries and size issues
        List<Reward> rewards = new ArrayList<>();

        // Taking from parser the dungeon section about rewards
        final ConfigurationSection section = dungeonConf.getConfigurationSection("rewards");

        // Checking if configuration has this section
        if (section == null) {
            return rewards;
        }

        // Taking subsections of rewards so each reward
        final Set<String> subSections = section.getKeys(false);

        // Check if there's some rewards registered
        if (subSections.isEmpty()) {
            return rewards;
        }

        // Iterating along all subsections
        for (final String s : subSections) {

            // Verifica esistenza sottosezione
            final ConfigurationSection rewardSection = section.getConfigurationSection(s);
            if (rewardSection == null) {
                continue;
            }

            // Validate experience before processing
            double exp = section.getDouble(s + ".experience");
            if (exp < 0) {
                continue;
            }

            // Get commands (may be empty - this is optional)
            List<String> cmds = section.getStringList(s + ".commands");

            // Parse items using ArrayList to avoid null entries
            List<ItemReward> itemsList = parseItems(section, s);

            // Create the reward with final variables
            final String rewardId = s;
            final double experience = exp;
            final List<String> commands = cmds;
            final List<ItemReward> itemRewards = itemsList;

            AbstractReward reward = new AbstractReward() {
                @Override
                public String id() {
                    return rewardId;
                }

                @Override
                public List<ItemReward> rewards() {
                    return itemRewards;
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

            rewards.add(reward);
        }

        return rewards;
    }

    /**
     * Parse items for a specific reward section
     */
    private List<ItemReward> parseItems(ConfigurationSection section, String rewardId) {
        List<ItemReward> itemsList = new ArrayList<>();

        final ConfigurationSection itemSec = section.getConfigurationSection(rewardId + ".items");

        // Checking if there's some items in this reward
        if (itemSec == null) {
            return itemsList;
        }

        final Set<String> items = itemSec.getKeys(false);
        if (items.isEmpty()) {
            return itemsList;
        }

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
            if (amount <= 0) {
                continue;
            }
            final boolean finalIsMythic = isMythic;
            final String finalMythicName = mythicName;
            final String finalType = type;
            final int finalAmount = amount;

            // Building up the items
            ItemReward itemReward = new ItemReward() {
                @Override
                public boolean isMythicItem() {
                    return finalIsMythic;
                }

                @Override
                public Optional<String> mythicItemName() {
                    return Optional.ofNullable(finalMythicName);
                }

                @Override
                public Optional<String> type() {
                    return Optional.of(finalType);
                }

                @Override
                public int amount() {
                    return finalAmount;
                }

                @Override
                public boolean isGlowing() {
                    return itemSec.getBoolean(itemPath + "is_glowing");
                }

                @Override
                public List<String> enchantments() {
                    return itemSec.getStringList(itemPath + "enchants");
                }

                @Override
                public String displayName() {
                    return itemSec.getString(itemPath + "display_name");
                }

                @Override
                public List<String> lore() {
                    return itemSec.getStringList(itemPath + "lore");
                }
            };

            itemsList.add(itemReward);
        }

        return itemsList;
    }
}