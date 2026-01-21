package com.littlesquad.dungeon.api.rewards;

import com.littlesquad.Main;
import com.littlesquad.dungeon.internal.utils.CommandUtils;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.experience.EXPSource;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.MMOItemsAPI;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public abstract class AbstractReward implements Reward {

    private final Map<String, ItemStack> cachedItems = new HashMap<>();


    public void give(final Player player) {


        if (cachedItems.isEmpty()) {
            cacheItemsRewards();
        }

        final UUID pu = player.getUniqueId();
        final PlayerData data = PlayerData.get(pu);

        Bukkit.getScheduler().runTask(Main.getInstance(),
                () -> data.giveExperience(experience(), EXPSource.SOURCE));

        CommandUtils.executeMulti(
                Bukkit.getConsoleSender(),
                commands(),
                player);

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            rewards()
                    .stream()
                    .filter(ItemReward::isMythicItem)
                    .forEach(r -> {

                        final MMOItem mmoitem = MMOItems
                                .plugin
                                .getMMOItem(MMOItems
                                                .plugin
                                                .getTypes()
                                                .get(r.type().orElseThrow()),
                                        r.mythicItemName().orElseThrow());

                        final ItemStack item = mmoitem.newBuilder().build();

                        player.getInventory().addItem(item);
                    });
        });

        for (ItemStack item : cachedItems.values()) {
            player.getInventory().addItem(item.clone());
        }
    }

    public void give(final Player... players) {
        for (Player player : players) {
            give(player);
        }
    }

    private void cacheItemsRewards() {
        int itemIndex = 0;
        for (final ItemReward reward : rewards()) {

            if (!reward.isMythicItem()) {

                final Material material = Material.getMaterial(reward.type().orElse("STONE"));

                if (material != null) {
                    ItemStack is = new ItemStack(material, reward.amount());
                    final ItemMeta meta = is.getItemMeta();

                    if (meta != null) {
                        if (reward.displayName() != null && !reward.displayName().isEmpty()) {
                            meta.displayName(Component.text(reward.displayName()));
                        }

                        if (!reward.lore().isEmpty()) {
                            List<Component> loreComponents = new ArrayList<>();
                            for (String loreLine : reward.lore()) {
                                loreComponents.add(Component.text(loreLine));
                            }
                            meta.lore(loreComponents);
                        }

                        for (String enchantStr : reward.enchantments()) {
                            try {
                                Enchantment enchant = Enchantment.getByName(enchantStr);
                                if (enchant != null) {
                                    meta.addEnchant(enchant, 1, true);
                                }
                            } catch (Exception e) {
                            }
                        }

                        if (reward.isGlowing() && reward.enchantments().isEmpty()) {
                            meta.addEnchant(Enchantment.LURE, 1, true);
                            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                        }

                        is.setItemMeta(meta);
                    }

                    cachedItems.put("item_" + itemIndex, is);
                }

            }

            itemIndex++;
        }
    }
}