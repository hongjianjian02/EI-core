package com.mysterious.mystic.compat.anvilguide.jei;

import com.mysterious.mystic.compat.anvilguide.recipe.AnvilBookItemsRecipe;
import com.mysterious.mystic.compat.anvilguide.ui.AnvilGuideLayoutText;
import com.mysterious.mystic.compat.anvilguide.ui.AnvilGuideText;
import com.mysterious.mystic.compat.anvilguide.util.TextUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/**
 * 附魔书页物品反查：输入一本附魔书，用一整块滚动网格列出所有能用它的物品。
 *
 * <p>物品按武器 / 工具 / 盔甲 / 其他依次排开，底部图例给出各类数量；
 * 全部内容都在同一块网格里滚动，不再按装备类型分页。</p>
 */
public class AnvilBookItemsCategory extends AbstractRecipeCategory<AnvilBookItemsRecipe> {

    private final IDrawable arrow;

    public AnvilBookItemsCategory(IGuiHelper guiHelper) {
        super(AnvilEnchantJeiPlugin.BOOK_ITEMS,
                Component.translatable("eicore.anvil_guide.category.book"),
                guiHelper.createDrawableItemStack(new ItemStack(Items.ENCHANTED_BOOK)),
                Layout.WIDTH, Layout.BOOK_HEIGHT);
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilBookItemsRecipe recipe, IFocusGroup focuses) {
        // 书页保留实际组件，R/U 入口由高级查询插件处理，避免展示格产生反向重复页。
        IRecipeSlotBuilder input = builder.addSlot(RecipeIngredientRole.RENDER_ONLY,
                Layout.INPUT_X, Layout.INPUT_Y).setSlotName("query");
        input.addItemStack(recipe.addon().stack());
        input.setStandardSlotBackground();
        StageTooltipCallbacks.add(input, recipe.addon().stack());
        input.addRichTooltipCallback((view, tooltip) ->
                AnvilGuideText.bookInputTooltip(recipe.books()).forEach(tooltip::add));

        for (ItemStack stack : recipe.items()) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.RENDER_ONLY);
            slot.addItemStack(stack);
            StageTooltipCallbacks.add(slot, stack);
            if (AnvilGuideText.hasItemConflicts(recipe.books(), recipe.addon().stack(), stack)) {
                slot.setOverlay(new ConflictBadge(), 0, 0);
            }
            slot.addRichTooltipCallback((view, tooltip) -> AnvilGuideText
                    .itemDetails(recipe.books(), recipe.addon().stack(), stack).tooltip().forEach(tooltip::add));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, AnvilBookItemsRecipe recipe, IFocusGroup focuses) {
        Layout.placeRenderOnlyScrollGrid(builder);
    }

    @Override
    public void draw(AnvilBookItemsRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, Layout.ARROW_X, Layout.ARROW_Y);

        Font font = Minecraft.getInstance().font;
        int lineY = Layout.STATS_TEXT_Y;
        for (Component line : AnvilGuideLayoutText.legendLines(recipe.groups(), font,
                Layout.WIDTH - Layout.TEXT_X * 2)) {
            guiGraphics.drawString(font, line, Layout.TEXT_X, lineY, Layout.COLOR_TEXT, false);
            lineY += Layout.TEXT_LINE_HEIGHT;
        }
        guiGraphics.drawString(font, Component.translatable("eicore.anvil_guide.stats.items", recipe.items().size()),
                Layout.TEXT_X, lineY + 1, Layout.COLOR_TEXT_DIM, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, AnvilBookItemsRecipe recipe, IRecipeSlotsView recipeSlotsView,
                           double mouseX, double mouseY) {
        if (TextUtils.isInside(mouseX, mouseY, Layout.TEXT_X, Layout.STATS_TEXT_Y - 2,
                Layout.WIDTH - Layout.TEXT_X * 2, Layout.TEXT_LINE_HEIGHT * 2 + 4)) {
            AnvilGuideText.bookLegendTooltip(recipe.addon().fullName()).forEach(tooltip::add);
        }
    }
}
