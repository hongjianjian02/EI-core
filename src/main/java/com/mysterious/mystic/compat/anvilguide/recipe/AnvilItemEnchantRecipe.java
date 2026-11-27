package com.mysterious.mystic.compat.anvilguide.recipe;

import java.util.List;

import com.mysterious.mystic.compat.anvilguide.data.BookEntry;
import com.mysterious.mystic.compat.anvilguide.data.EnchantmentCompat;
import net.minecraft.world.item.ItemStack;

/**
 * 「物品 → 可敲的附魔书」的共享查询模型。
 *
 * <p>折叠状态只列每种附魔的最高等级书；展开状态列出全部等级（最高级在前）。
 * 内容再多也只靠滚动条翻动，不再分页。</p>
 *
 * <p>书的顺序是「正面附魔 → 诅咒附魔」，诅咒段落从新的一行开始，
 * 它上面那行留作分隔带（见 {@link #sectionPadding(int, int)}）。</p>
 *
 * @param input      被附魔的物品
 * @param books      该物品能吃的全部附魔书（正面在前、诅咒在后）
 * @param expanded   是否已展开低等级
 * @param expandable 该物品是否存在低于最高等级的附魔书
 */
public record AnvilItemEnchantRecipe(ItemStack input, List<BookEntry> books, boolean expanded,
                                     boolean expandable) {

    /** 第一本诅咒附魔书在 {@link #books()} 里的下标；没有诅咒时返回 -1。 */
    public int firstCurseIndex() {
        for (int index = 0; index < books.size(); index++) {
            if (EnchantmentCompat.isCurse(books.get(index).enchantment())) {
                return index;
            }
        }
        return -1;
    }

    public int curseCount() {
        int first = firstCurseIndex();
        return first < 0 ? 0 : books.size() - first;
    }

    /** 是否要画分割线：正面附魔和诅咒附魔两边都有内容才有意义。 */
    public boolean separatesCurses() {
        return firstCurseIndex() > 0;
    }

    /**
     * 从第 {@code index} 格开始一个新段落需要的占位格数：
     * 先把当前行补满，再空出一整行放分隔带。
     */
    public static int sectionPadding(int index, int columns) {
        int remainder = index % columns;
        return (remainder == 0 ? 0 : columns - remainder) + columns;
    }

    /** 网格里实际占用的格子数（含分隔带与补齐用的占位格），用来判断要不要预留滚动条的位置。 */
    public int gridCells(int columns) {
        if (books.isEmpty()) {
            return 0;
        }
        return separatesCurses()
                ? firstCurseIndex() + sectionPadding(firstCurseIndex(), columns) + curseCount()
                : books.size();
    }
}
