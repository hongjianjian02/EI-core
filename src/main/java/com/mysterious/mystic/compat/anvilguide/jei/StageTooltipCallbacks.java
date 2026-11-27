package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.Collection;

import com.mysterious.mystic.compat.astages.ClientRecipeStageTooltips;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Keeps custom JEI grid slots consistent with EI core's stage-hint tooltips. */
final class StageTooltipCallbacks {
    private StageTooltipCallbacks() {
    }

    static void add(IRecipeSlotBuilder slot, ItemStack stack) {
        addDisplayedStack(slot);
    }

    static void add(IRecipeSlotBuilder slot, Collection<ItemStack> stacks) {
        addDisplayedStack(slot);
    }

    private static void addDisplayedStack(IRecipeSlotBuilder slot) {
        slot.addRichTooltipCallback((view, tooltip) -> {
            view.getDisplayedItemStack().ifPresent(stack ->
                    ClientRecipeStageTooltips.forItem(stack).forEach(tooltip::add));
        });
    }
}
