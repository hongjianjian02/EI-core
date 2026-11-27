package com.mysterious.mystic.compat.anvilguide.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 铁砧（含奥术铁砧）上能放在右侧格子的「附加物」：附魔书，或者 Iron's Spells 的法术卷轴。
 *
 * <p>物品页统一按这个类型排格子：先正面附魔书、再诅咒附魔书、最后法术卷轴，
 * 附魔书页也能用同一套网格把任意一种附加物反查成适用物品。</p>
 */
public interface AnvilAddon {

    /** 放进格子的物品堆。 */
    ItemStack stack();

    /** 带等级的完整名字，用于提示文字。 */
    Component fullName();

    /** 这格要标的等级（附魔等级 / 法术等级）。 */
    int level();
}
