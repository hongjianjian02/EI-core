package com.mysterious.mystic.compat.anvilguide.recipe;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * 「修复材料 → 它能修补的全部物品」的一条配方。
 *
 * <p>能修出同一批物品的材料（任意木板、任意石头材料……）并成一条，
 * 所以对着其中任何一种材料查看用途，看到的都是同一张合并后的表。</p>
 *
 * @param materials 这一组修复材料（每种都是一个独立的铁砧修补材料）
 * @param items     它们能修补耐久的所有物品
 */
public record AnvilRepairRecipe(List<ItemStack> materials, List<ItemStack> items) {
}
