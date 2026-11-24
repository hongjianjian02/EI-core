package com.mysterious.mystic.compat.astages;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class EmiStageIndicators {
    private static final ResourceLocation UNKNOWN_ITEM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "hardcoreastagesitem", "textures/item/unknown_item.png");

    private EmiStageIndicators() {
    }

    public static List<Component> itemHints(EmiIngredient ingredient) {
        if (!(ingredient instanceof ItemEmiStack item)) return List.of();
        return ClientRecipeStageTooltips.forItem(item.getItemStack());
    }

    public static Set<Component> resultHints(SlotWidget slot) {
        var recipe = slot.getRecipe();
        if (recipe == null) return Set.of();

        Set<Component> hints = new LinkedHashSet<>(itemHints(slot.getStack()));
        hints.addAll(ClientRecipeStageTooltips.forRecipe(recipe.getBackingRecipe()));
        return hints;
    }

    public static ClientTooltipComponent tooltipComponent(Component hint) {
        return ClientTooltipComponent.create(hint.getVisualOrderText());
    }

    public static void drawBadge(GuiGraphics graphics, int right, int top) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 300);
            graphics.blit(UNKNOWN_ITEM_TEXTURE, right - 9, top, 9, 9, 0, 0, 16, 16, 16, 16);
        } finally {
            graphics.pose().popPose();
        }
    }
}
