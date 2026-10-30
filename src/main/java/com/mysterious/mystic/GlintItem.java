package com.mysterious.mystic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 始终带附魔光效的物品，用于「激活」类道具。
 */
public class GlintItem extends Item {
    public GlintItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
