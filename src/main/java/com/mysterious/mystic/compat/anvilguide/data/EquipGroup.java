package com.mysterious.mystic.compat.anvilguide.data;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/** 附魔书适用物品的装备分组；未知的模组物品保留在其他组。 */
public enum EquipGroup {
    WEAPON,
    TOOL,
    ARMOR,
    OTHER;

    public String translationKey() {
        return "eicore.anvil_guide.group." + name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }

    public static EquipGroup classify(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ArmorItem || item instanceof ElytraItem || item instanceof AnimalArmorItem) {
            return ARMOR;
        }
        if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem
                || item instanceof BowItem || item instanceof CrossbowItem || item instanceof ShieldItem) {
            return WEAPON;
        }
        if (item instanceof DiggerItem || item instanceof FishingRodItem || item instanceof ShearsItem) {
            return TOOL;
        }
        return OTHER;
    }
}
