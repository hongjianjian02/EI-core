package com.mysterious.mystic.compat.anvilguide.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 一张法术卷轴（Iron's Spells 'n Spellbooks）。
 *
 * <p>卷轴物品本身只有一种，具体法术与等级写在物品数据里，所以这里直接存好造好的物品堆，
 * 和附魔书一样参与「物品页 / 附加物页」的网格排布。</p>
 *
 * @param stack    卷轴物品堆（缓存复用）
 * @param fullName 带法术等级的名字
 * @param level    法术等级
 */
public record ScrollEntry(ItemStack stack, Component fullName, int level) implements AnvilAddon {
}
