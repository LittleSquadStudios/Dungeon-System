package com.littlesquad.dungeon.api.rewards;

import io.lumine.mythic.bukkit.utils.items.Enchantments;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Optional;

public interface ItemReward {

    boolean isMythicItem();

    Optional<String> mythicItemName();
    Optional<String> type();

    int amount();
    boolean isGlowing();

    List<String> enchantments();

    String displayName();
    List<String> lore();

}
