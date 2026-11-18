package com.mysterious.mystic.foodbox;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public final class FlavorFoodRules {
    private FlavorFoodRules() {
    }

    public static boolean canTaste(Player player, ItemStack food) {
        if (food.isEmpty() || food.getFoodProperties(player) == null
                || food.getItem() instanceof FlavorBoxItem
                || food.has(FlavorBoxComponents.CONTENTS.get())
                || food.getItem() instanceof com.tarinoita.solsweetpotato.item.foodcontainer.FoodContainerItem
                || food.has(DataComponents.CONTAINER) || food.has(DataComponents.BUNDLE_CONTENTS)
                || food.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ITEM) != null
                || !food.canFitInsideContainerItems()) {
            return false;
        }
        UseAnim animation = food.getUseAnimation();
        if (animation != UseAnim.EAT && animation != UseAnim.DRINK) {
            return false;
        }
        return !com.cazsius.solcarrot.tracking.FoodList.get(player).getEatenFoods()
                .contains(new com.cazsius.solcarrot.tracking.FoodInstance(food.getItem()))
                && !com.tarinoita.solsweetpotato.tracking.FoodList.get(player).hasEaten(food.getItem());
    }
}
