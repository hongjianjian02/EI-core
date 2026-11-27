package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.List;

import com.mysterious.mystic.compat.anvilguide.data.IronsSpellsCompat;
import com.mysterious.mystic.compat.anvilguide.data.ScrollEntry;
import com.mysterious.mystic.compat.anvilguide.util.TextUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
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
 * 奥术铁砧页：法术卷轴与武器 / 装备的灌注关系，两种形态共用一个页签。
 *
 * <p>Iron's Spells 自带的奥术铁砧页签里混着卷轴自身升级、升级球升级物品、亲和指环灌注和卷轴灌注
 * 四类配方。本模组只接管卷轴灌注那一类：它在原页签是「一件物品一条配方、右侧列满全部等级的卷轴」，
 * 对着卷轴查看用途会冒出几百页。接管后的样子：</p>
 *
 * <ul>
 *   <li>合并页（第一页）：任意卷轴 + 全部可灌注物品。对着卷轴按 U 只看这一页，
 *       物品在这一页里合并展示。</li>
 *   <li>物品页：一件物品 + 它能用的卷轴（每种法术最高等级）。卷轴格只画不参与查询，
 *       所以对着卷轴按 R 只会看到卷轴自身升级（留在 Iron's 自己的页签里）。</li>
 * </ul>
 */
public class ArcaneImbueCategory extends AbstractRecipeCategory<ArcaneImbueRecipe> {

    private final IDrawable arrow;

    public ArcaneImbueCategory(IGuiHelper guiHelper, ItemStack icon) {
        super(AnvilEnchantJeiPlugin.ARCANE_IMBUE,
                Component.translatable("eicore.anvil_guide.category.arcane_anvil"),
                guiHelper.createDrawableItemStack(icon),
                Layout.WIDTH, Layout.ARCANE_HEIGHT);
        this.arrow = guiHelper.getRecipeArrow();
    }

    /** 页签图标：优先用奥术铁砧方块，模组没装或找不到方块时退回铁砧。 */
    public static ItemStack defaultIcon() {
        ItemStack anvil = IronsSpellsCompat.arcaneAnvilStack();
        return anvil.isEmpty() ? new ItemStack(Items.ANVIL) : anvil;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ArcaneImbueRecipe recipe, IFocusGroup focuses) {
        if (recipe.isMerged()) {
            setMergedRecipe(builder, recipe);
            return;
        }
        IRecipeSlotBuilder input = builder.addInputSlot(Layout.INPUT_X, Layout.INPUT_Y);
        input.addItemStack(recipe.input());
        input.setStandardSlotBackground();
        StageTooltipCallbacks.add(input, recipe.input());

        for (ScrollEntry scroll : recipe.scrolls()) {
            // 这些格子只画不参与查询：对着卷轴按 R 只该看到卷轴自身升级，按 U 只该看到合并页
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.RENDER_ONLY);
            slot.addItemStack(scroll.stack());
            StageTooltipCallbacks.add(slot, scroll.stack());
            slot.setOverlay(new LevelBadge(scroll.level()), 0, 0);
            slot.addRichTooltipCallback((view, tooltip) -> addScrollTooltip(tooltip, scroll, recipe));
        }
    }

    /** 合并页：输入格里放全部等级的卷轴（任意一张都算），右侧网格铺出全部可灌注物品。 */
    private void setMergedRecipe(IRecipeLayoutBuilder builder, ArcaneImbueRecipe recipe) {
        IRecipeSlotBuilder input = builder.addInputSlot(Layout.INPUT_X, Layout.INPUT_Y);
        for (ScrollEntry scroll : recipe.scrolls()) {
            input.addItemStack(scroll.stack());
        }
        input.setStandardSlotBackground();
        StageTooltipCallbacks.add(input, recipe.scrolls().stream().map(ScrollEntry::stack).toList());
        input.addRichTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.any_scroll",
                    recipe.scrolls().size()));
            tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.any_scroll_hint"));
        });

        for (ItemStack target : recipe.targets()) {
            // 位置由 createRecipeExtras 里的滚动网格统一排布
            IRecipeSlotBuilder slot = builder.addOutputSlot();
            slot.addItemStack(target);
            StageTooltipCallbacks.add(slot, target);
            slot.addRichTooltipCallback((view, tooltip) -> {
                tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.target",
                        target.getHoverName()));
                tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.imbue_target"));
            });
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ArcaneImbueRecipe recipe, IFocusGroup focuses) {
        RecipeIngredientRole role = recipe.isMerged()
                ? RecipeIngredientRole.OUTPUT
                : RecipeIngredientRole.RENDER_ONLY;
        List<IRecipeSlotDrawable> slots = builder.getRecipeSlots().getSlots(role);
        Layout.placeScrollGrid(builder, slots);
    }

    private void addScrollTooltip(ITooltipBuilder tooltip, ScrollEntry scroll, ArcaneImbueRecipe recipe) {
        tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.scroll", scroll.fullName()));
        tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.level", TextUtils.roman(scroll.level())));
        tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.scroll_hint"));
        tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.target", recipe.input().getHoverName()));
    }

    @Override
    public void draw(ArcaneImbueRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, Layout.ARROW_X, Layout.ARROW_Y);

        Font font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, Component.translatable(recipe.isMerged()
                                ? "eicore.anvil_guide.stats.imbue_targets"
                                : "eicore.anvil_guide.stats.imbue",
                        recipe.cellCount()),
                Layout.TEXT_X, Layout.STATS_TEXT_Y, Layout.COLOR_TEXT, false);
        guiGraphics.drawString(font, Component.translatable(recipe.isMerged()
                                ? "eicore.anvil_guide.hint.imbue_targets"
                                : "eicore.anvil_guide.hint.imbue"),
                Layout.TEXT_X, Layout.HINT_TEXT_Y, Layout.COLOR_TEXT_DIM, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ArcaneImbueRecipe recipe, IRecipeSlotsView recipeSlotsView,
                           double mouseX, double mouseY) {
        if (!TextUtils.isInside(mouseX, mouseY, Layout.TEXT_X, Layout.STATS_TEXT_Y - 2,
                Layout.WIDTH - Layout.TEXT_X * 2, Layout.TEXT_LINE_HEIGHT * 2 + 4)) {
            return;
        }
        if (recipe.isMerged()) {
            tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.arcane_merged_legend",
                    recipe.cellCount()));
            tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.arcane_merged_hint"));
            return;
        }
        tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.arcane_legend", recipe.input().getHoverName()));
        tooltip.add(Component.translatable("eicore.anvil_guide.tooltip.arcane_legend_hint"));
    }
}
