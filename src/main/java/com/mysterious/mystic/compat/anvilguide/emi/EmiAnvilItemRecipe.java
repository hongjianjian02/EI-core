package com.mysterious.mystic.compat.anvilguide.emi;

import java.util.ArrayList;
import java.util.List;

import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.data.BookEntry;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilItemEnchantRecipe;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRecipeFactory;
import com.mysterious.mystic.compat.anvilguide.ui.AnvilGuideLayoutText;
import com.mysterious.mystic.compat.anvilguide.ui.AnvilGuideText;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/** One item lookup, with all applicable books displayed in one scrollable grid. */
public final class EmiAnvilItemRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final long epoch;
    private final AnvilItemEnchantRecipe model;
    private final EmiStack query;

    public EmiAnvilItemRecipe(ResourceLocation id, AnvilItemEnchantRecipe model) {
        this.id = id;
        this.epoch = EmiViewEpoch.current();
        this.model = model;
        this.query = EmiAnvilView.stack(model.input());
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return AnvilEnchantEmiPlugin.ITEM_ENCHANT;
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
        // The query anchor also exposes this information page to R; grid cells are display-only.
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
        return EmiAnvilView.ITEM_HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        EpochGuardedWidgets guarded = new EpochGuardedWidgets(widgets, epoch);
        if (!EmiViewEpoch.isCurrent(epoch)) {
            return;
        }
        widgets = guarded;
        widgets.addSlot(query, EmiAnvilView.INPUT_X, EmiAnvilView.INPUT_Y);
        EmiAnvilView.addArrow(widgets);
        EmiAnvilGridWidget.add(widgets, bookCells());
        if (model.books().isEmpty()) {
            addEmptyBookMessage(widgets);
        }
        widgets.addText(Component.translatable("eicore.anvil_guide.stats.books", model.books().size()),
                EmiAnvilView.TEXT_X, EmiAnvilView.STATS_Y, EmiAnvilView.TEXT, false);
        String hint;
        if (model.books().isEmpty()) {
            hint = "eicore.anvil_guide.hint.item_repair_only";
        } else if (model.expanded()) {
            hint = "eicore.anvil_guide.hint.expanded";
        } else {
            hint = "eicore.anvil_guide.hint.folded";
        }
        widgets.addText(Component.translatable(hint),
                EmiAnvilView.TEXT_X, EmiAnvilView.HINT_Y, EmiAnvilView.DIM_TEXT, false);
        addRepairRow(widgets);
        if (model.expandable() && !model.books().isEmpty()) {
            widgets.add(new LevelToggleWidget());
        }
    }

    private void addEmptyBookMessage(WidgetHolder widgets) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(
                Component.translatable("eicore.anvil_guide.enchantments.none"), EmiAnvilView.GRID_WIDTH - 8);
        int y = EmiAnvilView.GRID_Y + (EmiAnvilView.GRID_HEIGHT - lines.size() * EmiAnvilView.TEXT_LINE_HEIGHT) / 2;
        for (FormattedCharSequence line : lines) {
            int x = EmiAnvilView.GRID_X + (EmiAnvilView.GRID_WIDTH - font.width(line)) / 2;
            widgets.addText(line, x, y, EmiAnvilView.DIM_TEXT, false);
            y += EmiAnvilView.TEXT_LINE_HEIGHT;
        }
    }

    private List<EmiAnvilGridWidget.Cell> bookCells() {
        List<EmiAnvilGridWidget.Cell> cells = new ArrayList<>(Math.max(model.gridCells(EmiAnvilView.COLUMNS), 0));
        int curseStart = model.firstCurseIndex();
        int positives = model.separatesCurses() ? curseStart : model.books().size();
        for (int index = 0; index < positives; index++) {
            addBookCell(cells, model.books().get(index));
        }
        if (model.separatesCurses()) {
            int padding = AnvilItemEnchantRecipe.sectionPadding(curseStart, EmiAnvilView.COLUMNS);
            for (int index = 0; index < padding; index++) {
                cells.add(index == padding - 1
                        ? EmiAnvilGridWidget.Cell.header(Component.translatable(
                                "eicore.anvil_guide.section.curses", model.curseCount()))
                        : EmiAnvilGridWidget.Cell.EMPTY);
            }
            for (int index = curseStart; index < model.books().size(); index++) {
                addBookCell(cells, model.books().get(index));
            }
        }
        return cells;
    }

    private void addBookCell(List<EmiAnvilGridWidget.Cell> cells, BookEntry book) {
        cells.add(EmiAnvilGridWidget.Cell.of(book.stack(), book.level(), false,
                AnvilGuideText.bookTooltip(model.input(), book)));
    }

    private void addRepairRow(WidgetHolder widgets) {
        List<ItemStack> materials = AnvilEnchantData.repairMaterials(model.input());
        if (materials.isEmpty()) {
            widgets.addText(Component.translatable("eicore.anvil_guide.repair.none"),
                    EmiAnvilView.TEXT_X, 107, EmiAnvilView.DIM_TEXT, false);
            return;
        }
        SlotWidget slot = widgets.addSlot(EmiIngredient.of(materials.stream().map(EmiAnvilView::stack).toList()), 5, 101);
        AnvilGuideText.repairLookupTooltip(materials).forEach(slot::appendTooltip);
        Font font = Minecraft.getInstance().font;
        Component label = AnvilGuideText.repairLabel();
        widgets.addText(label, 27, 107, EmiAnvilView.TEXT, false);
        int namesX = 31 + font.width(label);
        int maxWidth = EmiAnvilView.WIDTH - namesX - 4;
        if (maxWidth > 8) {
            widgets.addText(AnvilGuideLayoutText.repairNames(materials, font, maxWidth),
                    namesX, 107, EmiAnvilView.DIM_TEXT, false);
        }
    }

    private final class LevelToggleWidget extends Widget {
        @Override
        public Bounds getBounds() {
            return new Bounds(5, 44, 44, 14);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            EmiAnvilView.panel(graphics, 5, 44, 44, 14,
                    getBounds().contains(mouseX, mouseY) ? EmiAnvilView.BUTTON_HOVER : EmiAnvilView.BUTTON_FILL,
                    EmiAnvilView.BUTTON_BORDER);
            Component label = Component.translatable(model.expanded()
                    ? "eicore.anvil_guide.button.collapse" : "eicore.anvil_guide.button.expand");
            graphics.drawCenteredString(Minecraft.getInstance().font, label, 27, 47, EmiAnvilView.TEXT);
        }

        @Override
        public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
            return List.of(Component.translatable(model.expanded()
                            ? "eicore.anvil_guide.tooltip.collapse" : "eicore.anvil_guide.tooltip.expand"),
                    Component.translatable("eicore.anvil_guide.tooltip.expand_hint"))
                    .stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList();
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (button != 0 || !getBounds().contains(mouseX, mouseY) || !AnvilEnchantEmiPlugin.canQuery()) {
                return false;
            }
            AnvilItemEnchantRecipe target = AnvilRecipeFactory.itemRecipe(model.input(), !model.expanded());
            if (target.books().isEmpty()) {
                return false;
            }
            EmiAnvilGridWidget.clearInteraction();
            EmiApi.displayRecipe(new EmiAnvilItemRecipe(id, target));
            return true;
        }
    }
}
