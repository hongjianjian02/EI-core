package com.mysterious.mystic.compat.anvilguide.data;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;

/** 一本具体等级的附魔书；费用为普通铁砧的基础附魔费用，不包含修补代价和改名。 */
public record BookEntry(Holder<Enchantment> enchantment, int level, ItemStack stack, int xpCost)
        implements AnvilAddon {

    @Override
    public Component fullName() {
        return Enchantment.getFullname(enchantment, level);
    }

    public int maxLevel() {
        return AnvilEnchantData.maxLevel(enchantment);
    }

    public boolean isHighestLevel() {
        return level >= maxLevel();
    }
}
