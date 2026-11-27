package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.List;

import com.mysterious.mystic.compat.anvilguide.data.ScrollEntry;
import net.minecraft.world.item.ItemStack;

/**
 * 奥术铁砧页的一条配方，有两种形态：
 *
 * <ul>
 *   <li>普通页：一件物品 → 它能在奥术铁砧上灌注的全部法术卷轴。Iron's Spells 自己的页签是
 *       「一件物品一条配方、右侧塞满所有等级的卷轴」，这里按物品合并卷轴并只留最高等级
 *       （低等级卷轴只会灌出更弱的法术）。卷轴格只画不参与查询，所以对着卷轴查看配方
 *       只会看到卷轴自身升级。</li>
 *   <li>合并页：任意法术卷轴 → 全部可灌注物品，只注册一条。以前拿着卷轴查看用途会看到
 *       几百条「某件物品 → 附魔书式」的灌注配方，现在只剩这一页。</li>
 * </ul>
 *
 * @param input   能被灌注的物品；合并页为空堆
 * @param scrolls 普通页：该物品能用的卷轴（每种法术最高等级）；合并页：全部等级的卷轴
 * @param targets 合并页：全部可灌注物品；普通页为空列表
 */
public record ArcaneImbueRecipe(ItemStack input, List<ScrollEntry> scrolls, List<ItemStack> targets) {

    /** 一件物品 → 它能用的卷轴。 */
    public static ArcaneImbueRecipe forItem(ItemStack input, List<ScrollEntry> scrolls) {
        return new ArcaneImbueRecipe(input.copy(), scrolls, List.of());
    }

    /** 任意卷轴 → 全部可灌注物品（一页看全）。 */
    public static ArcaneImbueRecipe merged(List<ScrollEntry> scrolls, List<ItemStack> targets) {
        return new ArcaneImbueRecipe(ItemStack.EMPTY, scrolls, targets);
    }

    public boolean isMerged() {
        return !targets.isEmpty();
    }

    /** 这一页网格里要铺的格数。 */
    public int cellCount() {
        return isMerged() ? targets.size() : scrolls.size();
    }
}
