package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.ArrayList;
import java.util.List;

import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRepairRecipe;
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
 * 铁砧修复页：左格放修复材料，右侧滚动网格列出它能修补耐久的全部物品。
 *
 * <p>以前修复材料是挂在每件物品的配方里的输入槽，对着铁锭按 U 会冒出几百条「某件物品 → 附魔书」，
 * 想看的「铁锭能修哪些东西」反而被埋掉。现在反过来按材料建表，并且把修出同一批物品的材料
 * （任意木板、任意石头材料等）并成一条，对着任何一种材料查看用途都只看到这一页。</p>
 */
public class AnvilRepairCategory extends AbstractRecipeCategory<AnvilRepairRecipe> {

    private final IDrawable arrow;

    public AnvilRepairCategory(IGuiHelper guiHelper) {
        super(AnvilEnchantJeiPlugin.ANVIL_REPAIR,
                Component.translatable("eicore.anvil_guide.category.repair"),
                guiHelper.createDrawableItemStack(new ItemStack(Items.ANVIL)),
                Layout.WIDTH, Layout.REPAIR_HEIGHT);
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilRepairRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder input = builder.addInputSlot(Layout.INPUT_X, Layout.INPUT_Y);
        for (ItemStack material : recipe.materials()) {
            input.addItemStack(material);
        }
        input.setStandardSlotBackground();
        StageTooltipCallbacks.add(input, recipe.materials());
        input.addRichTooltipCallback((view, tooltip) -> tooltip.add(
                AnvilGuideText.repairMaterials(recipe.materials(), 6)));

        for (ItemStack stack : recipe.items()) {
            // 位置由 createRecipeExtras 里的滚动网格统一排布
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.RENDER_ONLY);
            slot.addItemStack(stack);
            StageTooltipCallbacks.add(slot, stack);
            slot.addRichTooltipCallback((view, tooltip) ->
                    AnvilGuideText.repairItemTooltip(stack).forEach(tooltip::add));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, AnvilRepairRecipe recipe, IFocusGroup focuses) {
        Layout.placeRenderOnlyScrollGrid(builder);
    }

    @Override
    public void draw(AnvilRepairRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, Layout.ARROW_X, Layout.ARROW_Y);

        Font font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, Component.translatable("eicore.anvil_guide.stats.repairable",
                        recipe.items().size()),
                Layout.TEXT_X, Layout.STATS_TEXT_Y, Layout.COLOR_TEXT, false);
        guiGraphics.drawString(font, Component.translatable("eicore.anvil_guide.hint.repair"),
                Layout.TEXT_X, Layout.HINT_TEXT_Y, Layout.COLOR_TEXT_DIM, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, AnvilRepairRecipe recipe, IRecipeSlotsView recipeSlotsView,
                           double mouseX, double mouseY) {
        if (!TextUtils.isInside(mouseX, mouseY, Layout.TEXT_X, Layout.STATS_TEXT_Y - 2,
                Layout.WIDTH - Layout.TEXT_X * 2, Layout.TEXT_LINE_HEIGHT * 2 + 4)) {
            return;
        }
        AnvilGuideText.repairLegendTooltip(recipe.materials()).forEach(tooltip::add);
    }

    /** 建表用：把数据层的合并结果原样包成配方。 */
    public static List<AnvilRepairRecipe> recipes() {
        List<AnvilRepairRecipe> recipes = new ArrayList<>();
        for (AnvilEnchantData.RepairGroup group : AnvilEnchantData.repairGroups()) {
            recipes.add(new AnvilRepairRecipe(group.materials(), group.items()));
        }
        return List.copyOf(recipes);
    }
}
