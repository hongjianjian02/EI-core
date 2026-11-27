package com.mysterious.mystic.compat.anvilguide.emi;

import java.util.ArrayList;
import java.util.List;

import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRepairRecipe;
import com.mysterious.mystic.compat.anvilguide.ui.AnvilGuideText;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A merged repair-material lookup; equipment cells do not enter EMI's output index. */
public final class EmiAnvilRepairRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final long epoch;
    private final AnvilRepairRecipe model;
    private final EmiIngredient query;

    public EmiAnvilRepairRecipe(ResourceLocation id, AnvilRepairRecipe model) {
        this.id = id;
        this.epoch = EmiViewEpoch.current();
        this.model = model;
        this.query = EmiIngredient.of(model.materials().stream().map(EmiAnvilView::stack).toList());
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VanillaEmiRecipeCategories.ANVIL_REPAIRING;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return EmiViewEpoch.isCurrent(epoch) ? List.of(query) : List.of();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of();
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public boolean hideCraftable() {
        return true;
    }

    @Override
    public int getDisplayWidth() {
        return EmiAnvilView.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return EmiAnvilView.REPAIR_HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        EpochGuardedWidgets guarded = new EpochGuardedWidgets(widgets, epoch);
        if (!EmiViewEpoch.isCurrent(epoch)) {
            return;
        }
        widgets = guarded;
        widgets.addSlot(query, EmiAnvilView.INPUT_X, EmiAnvilView.INPUT_Y)
                .appendTooltip(AnvilGuideText.repairMaterials(model.materials(), 6));
        EmiAnvilView.addArrow(widgets);
        List<EmiAnvilGridWidget.Cell> cells = new ArrayList<>();
        for (ItemStack stack : model.items()) {
            cells.add(EmiAnvilGridWidget.Cell.of(stack, 0, false, AnvilGuideText.repairItemTooltip(stack)));
        }
        EmiAnvilGridWidget.add(widgets, cells);
        widgets.addText(Component.translatable("eicore.anvil_guide.stats.repairable", model.items().size()),
                EmiAnvilView.TEXT_X, EmiAnvilView.STATS_Y, EmiAnvilView.TEXT, false);
        widgets.addText(Component.translatable("eicore.anvil_guide.hint.repair"),
                EmiAnvilView.TEXT_X, EmiAnvilView.HINT_Y, EmiAnvilView.DIM_TEXT, false);
        widgets.addTooltipText(AnvilGuideText.repairLegendTooltip(model.materials()),
                EmiAnvilView.TEXT_X, EmiAnvilView.STATS_Y - 2, EmiAnvilView.WIDTH - EmiAnvilView.TEXT_X * 2,
                EmiAnvilView.TEXT_LINE_HEIGHT * 2 + 4);
    }
}
