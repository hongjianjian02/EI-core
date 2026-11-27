package com.mysterious.mystic.compat.anvilguide.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.data.BookEntry;
import com.mysterious.mystic.compat.anvilguide.data.EquipGroup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 将当前连接的数据转换为浏览器共用的铁砧查询模型。 */
public final class AnvilRecipeFactory {

    /**
     * 同一附魔的各等级书共用同一份物品列表与图例。
     *
     * <p>以前每个等级都重新拼一遍全量列表，一本附魔 5 个等级就要复制 5 次，
     * 整合包里附魔一多，注册阶段光复制列表就占了可观的时间与内存。</p>
     */
    private static final Map<Holder<Enchantment>, BookItems> BOOK_ITEMS = new HashMap<>();

    private AnvilRecipeFactory() {
    }

    public static void invalidateCache() {
        BOOK_ITEMS.clear();
    }

    /** 铁砧物品页：这件物品能敲哪些附魔书。 */
    public static AnvilItemEnchantRecipe itemRecipe(ItemStack input, boolean expanded) {
        ItemStack current = AnvilEnchantData.rebindEnchantments(input);
        List<BookEntry> entries = expanded
                ? AnvilEnchantData.allBooks(current)
                : AnvilEnchantData.highestBooks(current);
        // entries 已经是不可变列表，直接复用，不再为每件物品复制一份
        return new AnvilItemEnchantRecipe(current.copy(), entries, expanded,
                AnvilEnchantData.hasLowerLevels(current));
    }

    /** 有可用附魔书或当前连接核实的修补材料时，物品信息页才有可浏览的内容。 */
    public static boolean hasItemInformation(AnvilItemEnchantRecipe recipe) {
        return !recipe.books().isEmpty() || !AnvilEnchantData.repairMaterials(recipe.input()).isEmpty();
    }

    public static AnvilBookItemsRecipe bookRecipe(Holder<Enchantment> enchantment, int level) {
        BookEntry book = AnvilEnchantData.bookEntry(enchantment, level);
        BookItems data = BOOK_ITEMS.computeIfAbsent(book.enchantment(), AnvilRecipeFactory::collectItems);
        return new AnvilBookItemsRecipe(book, data.items(), data.groups());
    }

    /**
     * 为实际物品列表中的书建立精确输入：多附魔书可将其中适用的一部分敲到目标上。
     * 使用原书组件参与查询，目标保留组件变体，不能以同 Item 的默认堆叠代替。
     */
    public static AnvilBookItemsRecipe bookRecipe(ItemStack input) {
        ItemStack book = AnvilEnchantData.rebindEnchantments(input).copyWithCount(1);
        if (!AnvilEnchantData.isAnvilBook(book)) {
            throw new IllegalArgumentException("物品没有普通铁砧实际读取的书附魔组件");
        }
        ItemEnchantments stored = EnchantmentHelper.getEnchantmentsForCrafting(book);
        List<BookEntry> entries = new ArrayList<>();
        for (var entry : stored.entrySet()) {
            entries.add(AnvilEnchantData.bookEntry(entry.getKey(), entry.getIntValue()));
        }

        Map<StackIdentity, ItemStack> targets = new LinkedHashMap<>();
        for (int index = 0; index < entries.size(); index++) {
            BookEntry entry = entries.get(index);
            for (ItemStack target : AnvilEnchantData.itemsForBook(entry.enchantment(), book)) {
                if (!AnvilEnchantData.processesBookEntry(target, index)) {
                    continue;
                }
                targets.putIfAbsent(new StackIdentity(target.getItem(), target.getComponentsPatch()), target);
            }
        }

        BookItems data = groupItems(List.copyOf(targets.values()));
        return new AnvilBookItemsRecipe(new AnvilBookItemsRecipe.ObservedBook(book),
                data.items(), data.groups(), entries);
    }

    private static BookItems collectItems(Holder<Enchantment> enchantment) {
        Map<EquipGroup, List<ItemStack>> grouped = AnvilEnchantData.groupedItems(enchantment);
        return flattenGroups(grouped);
    }

    private static BookItems groupItems(List<ItemStack> stacks) {
        Map<EquipGroup, List<ItemStack>> grouped = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            grouped.computeIfAbsent(EquipGroup.classify(stack), key -> new ArrayList<>()).add(stack);
        }
        return flattenGroups(grouped);
    }

    private static BookItems flattenGroups(Map<EquipGroup, List<ItemStack>> grouped) {
        List<ItemStack> items = new ArrayList<>();
        List<AnvilBookItemsRecipe.GroupCount> groups = new ArrayList<>();
        for (EquipGroup group : EquipGroup.values()) {
            List<ItemStack> stacks = grouped.getOrDefault(group, List.of());
            if (stacks.isEmpty()) {
                continue;
            }
            items.addAll(stacks);
            groups.add(new AnvilBookItemsRecipe.GroupCount(group, stacks.size()));
        }
        return new BookItems(List.copyOf(items), List.copyOf(groups));
    }

    record BookItems(List<ItemStack> items, List<AnvilBookItemsRecipe.GroupCount> groups) {
    }

    private record StackIdentity(Item item, DataComponentPatch components) {
    }
}
