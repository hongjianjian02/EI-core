package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.ArrayList;
import java.util.List;

import com.mysterious.mystic.compat.anvilguide.data.AnvilAddon;
import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.data.BookEntry;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilItemEnchantRecipe;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRecipeFactory;
import com.mysterious.mystic.compat.anvilguide.ui.AnvilGuideLayoutText;
import com.mysterious.mystic.compat.anvilguide.ui.AnvilGuideText;
import com.mysterious.mystic.compat.anvilguide.util.TextUtils;
import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/**
 * 物品页铁砧附魔视图：输入一件物品，用一整块滚动网格列出它能吃的全部附魔书。
 *
 * <p>默认每种附魔只显示最高等级，点左下角按钮可展开全部等级；
 * 不管多少本书都只靠右侧滚动条翻动，不分页。</p>
 *
 * <p>书的排法是「正面附魔 → 分隔带 → 诅咒附魔」。
 * 法术卷轴不在这里，改由奥术铁砧页签（{@link ArcaneImbueCategory}）按物品合并展示。
 * 底部单独给出这件物品在铁砧上的修补材料。</p>
 */
public class AnvilItemEnchantCategory extends AbstractRecipeCategory<AnvilItemEnchantRecipe> {

    private final IDrawable arrow;

    public AnvilItemEnchantCategory(IGuiHelper guiHelper) {
        super(AnvilEnchantJeiPlugin.ITEM_ENCHANT,
                Component.translatable("eicore.anvil_guide.category.item"),
                guiHelper.createDrawableItemStack(new ItemStack(Items.ANVIL)),
                Layout.WIDTH, Layout.ITEM_HEIGHT);
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilItemEnchantRecipe recipe, IFocusGroup focuses) {
        // 聚合页由高级查询插件按真实组件生成，展示槽不参与 JEI 的粗粒度物品索引。
        IRecipeSlotBuilder input = builder.addSlot(RecipeIngredientRole.RENDER_ONLY,
                Layout.INPUT_X, Layout.INPUT_Y).setSlotName("query");
        input.addItemStack(recipe.input());
        input.setStandardSlotBackground();
        StageTooltipCallbacks.add(input, recipe.input());

        addRepairSlot(builder, recipe);

        // 这些槽位不指定坐标，位置由下面的滚动网格统一排布
        for (AnvilCell cell : buildCells(recipe)) {
            if (cell.hasHeader()) {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY)
                        .setOverlay(new SectionHeader(cell.header(), cell.color()), 0, 0);
                continue;
            }
            AnvilAddon addon = cell.addon();
            if (addon == null) {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY);
                continue;
            }
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.RENDER_ONLY);
            slot.addItemStack(addon.stack());
            StageTooltipCallbacks.add(slot, addon.stack());
            slot.setOverlay(new LevelBadge(addon.level()), 0, 0);
            if (addon instanceof BookEntry book) {
                slot.addRichTooltipCallback((view, tooltip) ->
                        AnvilGuideText.bookTooltip(recipe.input(), book).forEach(tooltip::add));
            }
        }
    }

    /**
     * 把各个段落展开成一格格的内容：正面附魔、诅咒附魔（前置分隔带）。
     * 段落之间先补齐当前行，再空一整行画分隔带，保证每一段都从新的一行开始。
     */
    private List<AnvilCell> buildCells(AnvilItemEnchantRecipe recipe) {
        List<AnvilCell> cells = new ArrayList<>(Math.max(recipe.gridCells(Layout.COLUMNS), 0));
        int curseStart = recipe.firstCurseIndex();
        int positives = recipe.separatesCurses() ? curseStart : recipe.books().size();
        for (int index = 0; index < positives; index++) {
            cells.add(AnvilCell.of(recipe.books().get(index)));
        }
        if (recipe.separatesCurses()) {
            int padding = AnvilItemEnchantRecipe.sectionPadding(curseStart, Layout.COLUMNS);
            for (int index = 0; index < padding; index++) {
                cells.add(index == padding - 1
                        ? AnvilCell.header(Component.translatable("eicore.anvil_guide.section.curses",
                                recipe.curseCount()), Layout.COLOR_CURSE_TEXT)
                        : AnvilCell.spacer());
            }
            for (int index = curseStart; index < recipe.books().size(); index++) {
                cells.add(AnvilCell.of(recipe.books().get(index)));
            }
        }
        return cells;
    }

    /**
     * 能铁砧修补的物品，在底部多给一格修复材料。
     *
     * <p>这一格只画不参与查询：以前它是输入槽，对着铁锭按 U 会冒出几百条「某件物品 → 附魔书」，
     * 现在按材料建的合并表在 {@link AnvilRepairCategory} 里，这里再挂一份输入只会让查询结果更乱。</p>
     */
    private void addRepairSlot(IRecipeLayoutBuilder builder, AnvilItemEnchantRecipe recipe) {
        List<ItemStack> materials = AnvilEnchantData.repairMaterials(recipe.input());
        if (materials.isEmpty()) {
            return;
        }
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.RENDER_ONLY,
                Layout.REPAIR_SLOT_X, Layout.REPAIR_SLOT_Y).setSlotName("repair_material");
        for (ItemStack material : materials) {
            slot.addItemStack(material);
        }
        slot.setStandardSlotBackground();
        StageTooltipCallbacks.add(slot, materials);
        slot.addRichTooltipCallback((view, tooltip) ->
                AnvilGuideText.repairLookupTooltip(materials).forEach(tooltip::add));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, AnvilItemEnchantRecipe recipe, IFocusGroup focuses) {
        Layout.placeRenderOnlyScrollGrid(builder);
        if (recipe.expandable()) {
            builder.addInputHandler(new IJeiInputHandler() {
                @Override
                public ScreenRectangle getArea() {
                    return new ScreenRectangle(Layout.TOGGLE_X, Layout.TOGGLE_Y,
                            Layout.TOGGLE_WIDTH, Layout.TOGGLE_HEIGHT);
                }

                @Override
                public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                    InputConstants.Key key = input.getKey();
                    if (key.getType() != InputConstants.Type.MOUSE
                            || key.getValue() != InputConstants.MOUSE_BUTTON_LEFT
                            || AnvilEnchantJeiPlugin.runtime() == null) {
                        return false;
                    }
                    // JEI 在按下时仅模拟是否能处理，在同一区域松开时才执行切换。
                    return input.isSimulate() || toggle(recipe);
                }
            });
        }
    }

    @Override
    public void draw(AnvilItemEnchantRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, Layout.ARROW_X, Layout.ARROW_Y);

        Font font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, Component.translatable("eicore.anvil_guide.stats.books", recipe.books().size()),
                Layout.TEXT_X, Layout.STATS_TEXT_Y, Layout.COLOR_TEXT, false);
        guiGraphics.drawString(font, Component.translatable(recipe.expanded()
                        ? "eicore.anvil_guide.hint.expanded"
                        : "eicore.anvil_guide.hint.folded"),
                Layout.TEXT_X, Layout.HINT_TEXT_Y, Layout.COLOR_TEXT_DIM, false);

        drawRepairRow(guiGraphics, font, recipe);

        if (recipe.expandable()) {
            boolean hovered = TextUtils.isInside(mouseX, mouseY, Layout.TOGGLE_X, Layout.TOGGLE_Y,
                    Layout.TOGGLE_WIDTH, Layout.TOGGLE_HEIGHT);
            Layout.button(guiGraphics, Layout.TOGGLE_X, Layout.TOGGLE_Y,
                    Layout.TOGGLE_WIDTH, Layout.TOGGLE_HEIGHT, hovered);
            Component label = Component.translatable(recipe.expanded()
                    ? "eicore.anvil_guide.button.collapse"
                    : "eicore.anvil_guide.button.expand");
            guiGraphics.drawCenteredString(font, label, Layout.TOGGLE_X + Layout.TOGGLE_WIDTH / 2,
                    Layout.TOGGLE_Y + 3, Layout.COLOR_BUTTON_TEXT);
        }
    }

    /** 底部那行修复材料说明：有材料时槽位由 JEI 画，这里只写文字；修不了的物品直接标明。 */
    private void drawRepairRow(GuiGraphics guiGraphics, Font font, AnvilItemEnchantRecipe recipe) {
        List<ItemStack> materials = AnvilEnchantData.repairMaterials(recipe.input());
        if (materials.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("eicore.anvil_guide.repair.none"),
                    Layout.TEXT_X, Layout.REPAIR_TEXT_Y, Layout.COLOR_TEXT_DIM, false);
            return;
        }
        Component label = AnvilGuideText.repairLabel();
        guiGraphics.drawString(font, label, Layout.REPAIR_TEXT_X, Layout.REPAIR_TEXT_Y, Layout.COLOR_TEXT, false);

        int namesX = Layout.REPAIR_TEXT_X + font.width(label) + 4;
        int maxWidth = Layout.WIDTH - namesX - 4;
        if (maxWidth <= 8) {
            return;
        }
        guiGraphics.drawString(font, AnvilGuideLayoutText.repairNames(materials, font, maxWidth),
                namesX, Layout.REPAIR_TEXT_Y, Layout.COLOR_TEXT_DIM, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, AnvilItemEnchantRecipe recipe, IRecipeSlotsView recipeSlotsView,
                           double mouseX, double mouseY) {
        if (!recipe.expandable()) {
            return;
        }
        if (!TextUtils.isInside(mouseX, mouseY, Layout.TOGGLE_X, Layout.TOGGLE_Y,
                Layout.TOGGLE_WIDTH, Layout.TOGGLE_HEIGHT)) {
            return;
        }
        tooltip.add(Component.translatable(recipe.expanded()
                ? "eicore.anvil_guide.tooltip.collapse"
                : "eicore.anvil_guide.tooltip.expand"));
        tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.expand_hint"));
    }

    private boolean toggle(AnvilItemEnchantRecipe recipe) {
        IJeiRuntime runtime = AnvilEnchantJeiPlugin.runtime();
        if (runtime == null) {
            return false;
        }
        AnvilItemEnchantRecipe target = AnvilRecipeFactory.itemRecipe(recipe.input(), !recipe.expanded());
        if (target.books().isEmpty()) {
            return false;
        }
        try {
            runtime.getGuiRuntime().getRecipesGui().showRecipes(this, List.of(target), List.of());
            return true;
        } catch (RuntimeException exception) {
            AnvilEnchantJeiPlugin.LOGGER.warn("切换铁砧附魔书的等级显示失败", exception);
            return false;
        }
    }
}
