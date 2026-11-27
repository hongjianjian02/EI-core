package com.mysterious.mystic.compat.anvilguide.recipe;

import java.util.List;

import com.mysterious.mystic.compat.anvilguide.data.AnvilAddon;
import com.mysterious.mystic.compat.anvilguide.data.BookEntry;
import com.mysterious.mystic.compat.anvilguide.data.EquipGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 「附加物 → 适用物品」的整张表：一本附魔书 / 一张法术卷轴一个配方，
 * 全部物品放进同一个滚动网格，按武器 / 工具 / 盔甲 / 其他依次排开，
 * 靠滚动条查看，不再分页。
 *
 * @param addon  作为输入的附加物（附魔书或法术卷轴）
 * @param items  全部可用物品（同类装备排在一起）
 * @param groups 每种装备类型的数量，用于底部图例
 */
public record AnvilBookItemsRecipe(AnvilAddon addon, List<ItemStack> items, List<GroupCount> groups,
                                   List<BookEntry> books) {

    /** 单附魔书入口仍共享原来的列表；实际多附魔书保留每项附魔用于适用性提示。 */
    public AnvilBookItemsRecipe(AnvilAddon addon, List<ItemStack> items, List<GroupCount> groups) {
        this(addon, items, groups, addon instanceof BookEntry book ? List.of(book) : List.of());
    }

    public AnvilBookItemsRecipe {
        items = List.copyOf(items);
        groups = List.copyOf(groups);
        books = List.copyOf(books);
    }

    /** 实际物品列表中的书：保留完整组件，避免用单附魔书替代多附魔书参与查询。 */
    public record ObservedBook(ItemStack stack) implements AnvilAddon {
        @Override
        public Component fullName() {
            return stack.getHoverName();
        }

        @Override
        public int level() {
            return 0;
        }
    }

    /** 图标数量里的一类装备。 */
    public record GroupCount(EquipGroup group, int count) {
    }
}
