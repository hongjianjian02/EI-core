package com.mysterious.mystic.compat.astages;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.emi.runtime.EmiTagKey;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

public final class EmiStageIndicators {
    private EmiStageIndicators() {
    }

    public static List<Component> itemHints(EmiIngredient ingredient) {
        return List.copyOf(indicator(ingredient, true).hints());
    }

    public static Set<Component> resultHints(SlotWidget slot) {
        return indicator(slot.getStack(), true).withRecipe(slot.getRecipe(), true).hints();
    }

    public static void drawItemBadge(GuiGraphics graphics, EmiIngredient ingredient, int iconX, int iconY) {
        if (indicator(ingredient, false).locked()) StageBadgeRenderer.draw(graphics, iconX, iconY);
    }

    public static void drawResultBadge(GuiGraphics graphics, SlotWidget slot) {
        if (!indicator(slot.getStack(), false).withRecipe(slot.getRecipe(), false).locked()) return;
        var bounds = slot.getBounds();
        int iconX = bounds.x() + (bounds.width() - 16) / 2;
        int iconY = bounds.y() + (bounds.height() - 16) / 2;
        StageBadgeRenderer.draw(graphics, iconX, iconY);
    }

    public static ClientTooltipComponent tooltipComponent(Component hint) {
        return ClientTooltipComponent.create(hint.getVisualOrderText());
    }

    private static Indicator indicator(EmiIngredient ingredient, boolean includeHints) {
        if (ingredient == null || ingredient.isEmpty()) return Indicator.NONE;
        if (ingredient instanceof EmiFavorite favorite) {
            return indicator(favorite.getStack(), includeHints).withRecipe(favorite.getRecipe(), includeHints);
        }
        if (ingredient instanceof ListEmiIngredient list) {
            var candidates = list.getIngredients();
            if (candidates.isEmpty()) return Indicator.NONE;
            // EMI cycles top-level ingredients, which may themselves be tags or lists.
            int index = (int) ((System.currentTimeMillis() / 1000) % candidates.size());
            return indicator(candidates.get(index), includeHints);
        }
        if (ingredient instanceof TagEmiIngredient tag && EmiTagKey.of(tag.key).hasCustomModel()) {
            // A custom tag model represents the whole tag rather than one displayed item.
            // Only mark it if every candidate is locked, so an unlocked alternative keeps it usable.
            Set<Component> hints = includeHints ? new LinkedHashSet<>() : Set.of();
            for (var candidate : tag.getEmiStacks()) {
                var candidateIndicator = stackIndicator(candidate, includeHints);
                if (!candidateIndicator.locked()) return Indicator.NONE;
                if (includeHints) hints.addAll(candidateIndicator.hints());
            }
            return includeHints ? new Indicator(true, hints) : Indicator.LOCKED;
        }
        if (ingredient instanceof EmiStack stack) return stackIndicator(stack, includeHints);
        // Native tags without a custom model display their first stack, not a cycling candidate.
        var stacks = ingredient.getEmiStacks();
        return stacks.isEmpty() ? Indicator.NONE : stackIndicator(stacks.getFirst(), includeHints);
    }

    private static Indicator stackIndicator(EmiStack ingredient, boolean includeHints) {
        var stack = ingredient.getItemStack();
        if (!ClientRecipeStageTooltips.isItemLocked(stack)) return Indicator.NONE;
        return includeHints
                ? new Indicator(true, new LinkedHashSet<>(ClientRecipeStageTooltips.forItem(stack)))
                : Indicator.LOCKED;
    }

    private record Indicator(boolean locked, Set<Component> hints) {
        private static final Indicator NONE = new Indicator(false, Set.of());
        private static final Indicator LOCKED = new Indicator(true, Set.of());

        private Indicator withRecipe(EmiRecipe recipe, boolean includeHints) {
            if (recipe == null) return this;
            var backingRecipe = recipe.getBackingRecipe();
            if (!ClientRecipeStageTooltips.isRecipeLocked(backingRecipe)) return this;
            if (!includeHints) return LOCKED;
            Set<Component> combined = new LinkedHashSet<>(hints);
            combined.addAll(ClientRecipeStageTooltips.forRecipe(backingRecipe));
            return new Indicator(true, combined);
        }
    }
}
