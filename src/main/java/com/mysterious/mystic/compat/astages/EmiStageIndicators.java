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
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

public final class EmiStageIndicators {
    private EmiStageIndicators() {
    }

    public static List<Component> itemHints(EmiIngredient ingredient) {
        return List.copyOf(indicator(ingredient).hints());
    }

    public static Set<Component> resultHints(SlotWidget slot) {
        return indicator(slot.getStack()).withRecipe(slot.getRecipe()).hints();
    }

    public static ClientTooltipComponent tooltipComponent(Component hint) {
        return ClientTooltipComponent.create(hint.getVisualOrderText());
    }

    private static Indicator indicator(EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return Indicator.NONE;
        if (ingredient instanceof EmiFavorite favorite) {
            return indicator(favorite.getStack()).withRecipe(favorite.getRecipe());
        }
        if (ingredient instanceof ListEmiIngredient list) {
            var candidates = list.getIngredients();
            if (candidates.isEmpty()) return Indicator.NONE;
            // EMI cycles top-level ingredients, which may themselves be tags or lists.
            int index = (int) ((System.currentTimeMillis() / 1000) % candidates.size());
            return indicator(candidates.get(index));
        }
        if (ingredient instanceof TagEmiIngredient tag && EmiTagKey.of(tag.key).hasCustomModel()) {
            // A custom tag model represents the whole tag rather than one displayed item.
            // Only show hints if every candidate is locked, so an unlocked alternative keeps it usable.
            Set<Component> hints = new LinkedHashSet<>();
            for (var candidate : tag.getEmiStacks()) {
                var candidateIndicator = stackIndicator(candidate);
                if (!candidateIndicator.locked()) return Indicator.NONE;
                hints.addAll(candidateIndicator.hints());
            }
            return new Indicator(true, hints);
        }
        if (ingredient instanceof EmiStack stack) return stackIndicator(stack);
        // Native tags without a custom model display their first stack, not a cycling candidate.
        var stacks = ingredient.getEmiStacks();
        return stacks.isEmpty() ? Indicator.NONE : stackIndicator(stacks.getFirst());
    }

    private static Indicator stackIndicator(EmiStack ingredient) {
        var stack = ingredient.getItemStack();
        if (!ClientRecipeStageTooltips.isItemLocked(stack)) return Indicator.NONE;
        return new Indicator(true, new LinkedHashSet<>(ClientRecipeStageTooltips.forItem(stack)));
    }

    private record Indicator(boolean locked, Set<Component> hints) {
        private static final Indicator NONE = new Indicator(false, Set.of());

        private Indicator withRecipe(EmiRecipe recipe) {
            if (recipe == null) return this;
            var backingRecipe = recipe.getBackingRecipe();
            if (!ClientRecipeStageTooltips.isRecipeLocked(backingRecipe)) return this;
            Set<Component> combined = new LinkedHashSet<>(hints);
            combined.addAll(ClientRecipeStageTooltips.forRecipe(backingRecipe));
            return new Indicator(true, combined);
        }
    }
}
