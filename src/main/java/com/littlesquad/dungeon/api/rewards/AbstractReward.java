package com.littlesquad.dungeon.api.rewards;

import com.littlesquad.Main;
import com.littlesquad.dungeon.placeholder.PlaceholderFormatter;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.experience.EXPSource;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public abstract class AbstractReward implements Reward {

    public void give(final Player player) {
        final UUID pu = player.getUniqueId();
        final PlayerData data = PlayerData.get(pu);

        // Experience
        data.giveExperience(experience(), EXPSource.SOURCE);

        // Commands
        commands().forEach(cmd ->
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    PlaceholderFormatter.formatPerPlayer(cmd, player)
                )
        );

        for (final ItemReward reward : rewards()) { // TODO: Migrate to caching system of rewards

            if (reward.isMythicItem()) {

                player.give(new MMOItem
                        (
                            Type.get
                                    (reward.type().orElseThrow()),
                            reward.mythicItemName()
                                    .orElseThrow()
                        )
                        .newBuilder()
                        .getItemStack());

            } else {

                final Material material = Material.getMaterial(reward.type().orElse("STONE"));
                ItemStack is;

                if (material != null) {
                    is = new ItemStack(material, reward.amount());

                    final ItemMeta meta = is.getItemMeta();

                    meta.lore()
                            .addAll(reward.lore()
                                    .stream()
                                    .map(Component::text)
                                    .toList());



                }

            }

        }

    }

    public void give(final Player... players) {
        for (Player player : players) {
            give(player);
        }
    }

    private void cacheRewards() {

    }

}
