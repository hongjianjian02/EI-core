package com.mysterious.mystic.compat.anvilguide.emi;

import java.util.ArrayList;
import java.util.List;

import com.mysterious.mystic.compat.anvilguide.recipe.AnvilBookItemsRecipe;
import com.mysterious.mystic.compat.anvilguide.ui.AnvilGuideLayoutText;
import com.mysterious.mystic.compat.anvilguide.ui.AnvilGuideText;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** One exact book, including every stored enchantment, and its compatible equipment. */
public final class EmiAnvilBookRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final long epoch;
    private final AnvilBookItemsRecipe model;
    private final EmiStack query;

    public EmiAnvilBookRecipe(ResourceLocation id, AnvilBookItemsRecipe model) {
        this.id = id;
        this.epoch = EmiViewEpoch.current();
        this.model = model;
        this.query = EmiAnvilView.stack(model.addon().stack());
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return AnvilEnchantEmiPlugin.BOOK_ITEMS;
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
        return EmiViewEpoch.isCurrent(epoch) ? List.of(query) : List.of();
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
        return EmiAnvilView.BOOK_HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        EpochGuardedWidgets guarded = new EpochGuardedWidgets(widgets, epoch);
        if (!EmiViewEpoch.isCurrent(epoch)) {
            return;
        }
        widgets = guarded;
        SlotWidget input = widgets.addSlot(query, EmiAnvilView.INPUT_X, EmiAnvilView.INPUT_Y);
        AnvilGuideText.bookInputTooltip(model.books()).forEach(input::appendTooltip);
        EmiAnvilView.addArrow(widgets);
        List<EmiAnvilGridWidget.Cell> cells = new ArrayList<>();
        for (ItemStack stack : model.items()) {
            AnvilGuideText.ItemDetails details = AnvilGuideText.itemDetails(model.books(), model.addon().stack(), stack);
            cells.add(EmiAnvilGridWidget.Cell.of(stack, 0, details.conflicts(), details.tooltip()));
        }
        EmiAnvilGridWidget.add(widgets, cells);
        int y = EmiAnvilView.STATS_Y;
        for (Component line : AnvilGuideLayoutText.legendLines(model.groups(), Minecraft.getInstance().font,
                EmiAnvilView.WIDTH - EmiAnvilView.TEXT_X * 2)) {
            widgets.addText(line, EmiAnvilView.TEXT_X, y, EmiAnvilView.TEXT, false);
            y += EmiAnvilView.TEXT_LINE_HEIGHT;
        }
        widgets.addText(Component.translatable("eicore.anvil_guide.stats.items", model.items().size()),
                EmiAnvilView.TEXT_X, y + 1, EmiAnvilView.DIM_TEXT, false);
        widgets.addTooltipText(AnvilGuideText.bookLegendTooltip(model.addon().fullName()),
                EmiAnvilView.TEXT_X, EmiAnvilView.STATS_Y - 2, EmiAnvilView.WIDTH - EmiAnvilView.TEXT_X * 2,
                EmiAnvilView.TEXT_LINE_HEIGHT * 2 + 4);
    }
}
