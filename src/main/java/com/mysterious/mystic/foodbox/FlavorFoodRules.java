package com.mysterious.mystic.foodbox;

import com.cazsius.solcarrot.tracking.FoodInstance;
import com.cazsius.solcarrot.tracking.FoodList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
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
        return !carrotHasEaten(player, food)
                && !com.tarinoita.solsweetpotato.tracking.FoodList.get(player).hasEaten(food.getItem());
    }

    private static boolean carrotHasEaten(Player player, ItemStack food) {
        FoodList list = FoodList.get(player);
        if (list.getEatenFoodCount() == 0) {
            return false;
        }
        // Carrot's fast lookup rejects foods without food properties for a null eater.
        try {
            if (food.getFoodProperties(null) != null) {
                return list.hasEaten(food);
            }
        } catch (RuntimeException requiresEater) {
            // Eligibility with the real player was already checked above.
        }
        return list.getEatenFoods().contains(new FoodInstance(food.getItem()));
    }
}
