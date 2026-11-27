package com.mysterious.mystic.compat.anvilguide.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.data.BookEntry;
import com.mysterious.mystic.compat.anvilguide.data.EquipGroup;
import com.mysterious.mystic.compat.anvilguide.util.TextUtils;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;

/** Shared anvil-guide information; callers own tooltip timing and browser presentation. */
public final class AnvilGuideText {
    private AnvilGuideText() {
    }

    public static List<Component> bookTooltip(ItemStack target, BookEntry entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("eicore.anvil_guide.tooltip.enchantment",
                TextUtils.enchantmentName(entry.enchantment())));
        lines.add(Component.translatable("eicore.anvil_guide.tooltip.level", TextUtils.roman(entry.level())));
        if (AnvilEnchantData.currentHolder(entry.enchantment()).isEmpty()) {
            lines.add(Component.translatable("eicore.anvil_guide.hint.refreshing"));
            return List.copyOf(lines);
        }
        lines.add(Component.translatable("eicore.anvil_guide.tooltip.cost",
                AnvilEnchantData.anvilCost(entry.enchantment(), entry.level(), target)));
        lines.add(Component.translatable("eicore.anvil_guide.tooltip.cost_hint"));
        addObservedLevelTooltip(lines, entry);
        lines.add(Component.translatable(entry.isHighestLevel()
                ? "eicore.anvil_guide.tooltip.highest_level"
                : "eicore.anvil_guide.tooltip.lower_level", TextUtils.roman(entry.maxLevel())));
        List<Holder<Enchantment>> conflicts = AnvilEnchantData.conflictsFor(entry.enchantment(), target);
        if (!conflicts.isEmpty()) {
            lines.add(Component.translatable("eicore.anvil_guide.tooltip.conflicts",
                    TextUtils.enchantmentNames(conflicts, 4)));
        }
        lines.add(Component.translatable("eicore.anvil_guide.tooltip.target", target.getHoverName()));
        return List.copyOf(lines);
    }

    public static List<Component> bookInputTooltip(List<BookEntry> books) {
        List<Component> lines = new ArrayList<>();
        if (books.size() > 1) {
            lines.add(Component.translatable("eicore.anvil_guide.tooltip.multi_book", books.size()));
        }
        for (BookEntry book : books) {
            addObservedLevelTooltip(lines, book);
        }
        return List.copyOf(lines);
    }

    private static void addObservedLevelTooltip(List<Component> lines, BookEntry entry) {
        int definedMax = entry.enchantment().value().getMaxLevel();
        if (entry.level() > definedMax) {
            lines.add(Component.translatable("eicore.anvil_guide.tooltip.observed_level",
                    TextUtils.roman(definedMax)));
        }
    }

    public static ItemDetails itemDetails(List<BookEntry> books, ItemStack sourceBook, ItemStack target) {
        List<Component> lines = new ArrayList<>();
        lines.add(equipmentGroup(target));
        List<BookEntry> applicable = applicableBooks(books, sourceBook, target);
        boolean poweredUnit = AnvilEnchantData.isPoweredEnchantmentUnit(target);
        if (books.size() > 1) {
            lines.add(Component.translatable("eicore.anvil_guide.tooltip.book_applicable",
                    TextUtils.enchantmentNames(applicable.stream().map(BookEntry::enchantment).toList(), 6)));
            if (!poweredUnit) {
                lines.add(Component.translatable("eicore.anvil_guide.tooltip.book_partial"));
            }
        }
        if (poweredUnit) {
            lines.add(Component.translatable("eicore.anvil_guide.tooltip.module_first"));
        }
        List<Holder<Enchantment>> conflicts = conflictsFor(applicable, target);
        if (conflicts.isEmpty()) {
            lines.add(Component.translatable("eicore.anvil_guide.tooltip.no_mutex"));
        } else {
            lines.add(Component.translatable("eicore.anvil_guide.tooltip.mutex",
                    TextUtils.enchantmentNames(conflicts, 4)));
        }
        List<ItemStack> materials = AnvilEnchantData.repairMaterials(target);
        if (!materials.isEmpty()) {
            lines.add(repairMaterials(materials, 3));
        }
        return new ItemDetails(!conflicts.isEmpty(), List.copyOf(lines));
    }

    /** JEI computes the badge during layout creation, while its tooltip remains lazy. */
    public static boolean hasItemConflicts(List<BookEntry> books, ItemStack sourceBook, ItemStack target) {
        return !conflictsFor(applicableBooks(books, sourceBook, target), target).isEmpty();
    }

    private static List<BookEntry> applicableBooks(List<BookEntry> books, ItemStack sourceBook, ItemStack target) {
        List<BookEntry> result = new ArrayList<>();
        for (int index = 0; index < books.size(); index++) {
            if (!AnvilEnchantData.processesBookEntry(target, index)) {
                break;
            }
            BookEntry entry = books.get(index);
            if (AnvilEnchantData.canApply(entry.enchantment(), target, sourceBook)) {
                result.add(entry);
            }
        }
        return result;
    }

    private static List<Holder<Enchantment>> conflictsFor(List<BookEntry> applicable, ItemStack target) {
        Set<Holder<Enchantment>> conflicts = new LinkedHashSet<>();
        for (BookEntry entry : applicable) {
            conflicts.addAll(AnvilEnchantData.conflictsFor(entry.enchantment(), target));
        }
        return List.copyOf(conflicts);
    }

    public static Component repairLabel() {
        return Component.translatable("eicore.anvil_guide.repair.label");
    }

    public static Component repairMaterials(List<ItemStack> materials, int limit) {
        return Component.translatable("eicore.anvil_guide.tooltip.repair_material",
                TextUtils.itemNames(materials, limit));
    }

    public static List<Component> repairLookupTooltip(List<ItemStack> materials) {
        return List.of(repairMaterials(materials, 6),
                Component.translatable("eicore.anvil_guide.tooltip.repair_hint"),
                Component.translatable("eicore.anvil_guide.tooltip.repair_lookup"));
    }

    public static List<Component> repairItemTooltip(ItemStack target) {
        return List.of(equipmentGroup(target),
                Component.translatable("eicore.anvil_guide.tooltip.repair_hint"));
    }

    public static List<Component> bookLegendTooltip(Component bookName) {
        return List.of(Component.translatable("eicore.anvil_guide.tooltip.legend", bookName),
                Component.translatable("eicore.anvil_guide.tooltip.legend_hint"));
    }

    public static List<Component> repairLegendTooltip(List<ItemStack> materials) {
        return List.of(Component.translatable("eicore.anvil_guide.tooltip.repair_legend",
                TextUtils.itemNames(materials, 6)),
                Component.translatable("eicore.anvil_guide.tooltip.repair_legend_hint"));
    }

    private static Component equipmentGroup(ItemStack target) {
        return Component.translatable("eicore.anvil_guide.tooltip.group", EquipGroup.classify(target).displayName());
    }

    public record ItemDetails(boolean conflicts, List<Component> tooltip) {
    }
}
