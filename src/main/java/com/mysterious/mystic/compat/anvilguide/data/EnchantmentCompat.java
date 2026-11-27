package com.mysterious.mystic.compat.anvilguide.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/** 1.21 的附魔标签与已经核实的特殊铁砧入口。 */
public final class EnchantmentCompat {

    private static final ResourceLocation POWERED_MODULE = ResourceLocation.fromNamespaceAndPath(
            "enchantablemekasuit", "module_powered_enchantment_unit");

    private EnchantmentCompat() {
    }

    public static boolean isCurse(Holder<Enchantment> enchantment) {
        return AnvilEnchantData.currentHolder(enchantment)
                .map(current -> current.is(EnchantmentTags.CURSE)).orElse(false);
    }

    /** 这里只检查引用已绑定；所属连接与当前定义统一由 AnvilEnchantData.currentHolder 校验。 */
    public static boolean isAvailable(Holder<Enchantment> enchantment) {
        return enchantment != null && enchantment.isBound();
    }

    public static boolean canApply(ItemStack stack, Holder<Enchantment> enchantment, ItemStack book) {
        Holder<Enchantment> current = AnvilEnchantData.currentHolder(enchantment).orElse(null);
        if (current == null || stack.isEmpty()) {
            return false;
        }
        if (isPoweredModule(stack)) {
            // Enchantable MekaSuit 1.0.3 的 AnvilUpdateEvent 直接接收附魔书的第一个附魔。
            // 仅对这个模块生效，MekaSuit 护甲仍遵循它自己的 supportsEnchantment 判定。
            return book.is(Items.ENCHANTED_BOOK)
                    && !book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
        }
        return EnchantmentHelper.canStoreEnchantments(stack)
                && stack.supportsEnchantment(current) && stack.isBookEnchantable(book);
    }

    public static boolean ignoresConflicts(ItemStack stack) {
        return isPoweredModule(stack);
    }

    public static boolean isPoweredModule(ItemStack stack) {
        return !stack.isEmpty() && POWERED_MODULE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
