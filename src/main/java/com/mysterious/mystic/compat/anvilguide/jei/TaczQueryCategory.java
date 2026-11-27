package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.function.Supplier;
import java.util.List;

import com.mysterious.mystic.compat.anvilguide.data.TaczCompat;
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
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/**
 * 永恒枪械工坊：零的三个查询页签，界面与铁砧附魔页同款：左侧输入格 + 箭头 + 右侧 6×4 滚动网格。
 *
 * <ul>
 *   <li>弹药适用枪械：输入弹药，网格里是全部能用它的枪械（按枪种分段）</li>
 *   <li>枪械配件查询：输入枪械，网格里是它支持的全部配件（按配件位置分段），
 *       输入格正下方再放一格它用的弹药</li>
 *   <li>配件查询：输入配件，网格里是全部能装它的枪械（按枪种分段）</li>
 * </ul>
 *
 * <p>TACZ 自己那四个「一条配方一页」的查询页签会被插件整体隐藏，改由这三页合并展示。</p>
 */
public class TaczQueryCategory extends AbstractRecipeCategory<TaczQueryRecipe> {

    private final TaczQueryRecipe.Kind kind;
    private final IDrawable arrow;

    /**
     * @param type 这一页签的配方类型
     * @param kind 页签的三种形态之一
     * @param icon 页签图标：TACZ 原来的枪械 / 弹药 / 配件物品，数据没加载完时退回原始物品
     */
    public TaczQueryCategory(IGuiHelper guiHelper, RecipeType<TaczQueryRecipe> type, TaczQueryRecipe.Kind kind,
                             Supplier<ItemStack> icon) {
        super(type, Component.translatable(kind.titleKey()), lazyIcon(guiHelper, icon),
                Layout.WIDTH, Layout.TACZ_HEIGHT);
        this.kind = kind;
        this.arrow = guiHelper.getRecipeArrow();
    }

    /**
     * 页签图标延迟到第一次绘制时再取。
     *
     * <p>JEI 注册分类的阶段早于 TACZ 的数据包读完，那时还拿不到「任意一把枪」，
     * 而页签图标只在配方界面里画，那时候数据一定已经就绪。取不到时用 TACZ 的原始物品、
     * 再不行用火药兜底，保证页签不会画成空白。</p>
     */
    private static IDrawable lazyIcon(IGuiHelper guiHelper, Supplier<ItemStack> source) {
        return new IDrawable() {
            private IDrawable delegate;

            private IDrawable delegate() {
                if (delegate == null) {
                    ItemStack stack = source.get();
                    if (stack == null || stack.isEmpty()) {
                        stack = TaczCompat.firstGunStack();
                    }
                    if (stack.isEmpty()) {
                        stack = new ItemStack(Items.GUNPOWDER);
                    }
                    delegate = guiHelper.createDrawableItemStack(stack);
                }
                return delegate;
            }

            @Override
            public int getWidth() {
                return 16;
            }

            @Override
            public int getHeight() {
                return 16;
            }

            @Override
            public void draw(GuiGraphics guiGraphics, int x, int y) {
                delegate().draw(guiGraphics, x, y);
            }
        };
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TaczQueryRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder input = builder.addInputSlot(Layout.INPUT_X, Layout.INPUT_Y);
        input.addItemStack(recipe.input());
        input.setStandardSlotBackground();
        StageTooltipCallbacks.add(input, recipe.input());
        Component inputTooltip = recipe.inputTooltip();
        if (inputTooltip != null) {
            input.addRichTooltipCallback((view, tooltip) -> tooltip.add(inputTooltip));
        }

        addAmmoSlot(builder, recipe);

        for (TaczQueryRecipe.Cell cell : recipe.cells()) {
            if (cell.isHeader()) {
                builder.addOutputSlot().setOverlay(new SectionHeader(cell.header(), Layout.COLOR_TACZ_SECTION), 0, 0);
                continue;
            }
            IRecipeSlotBuilder slot = builder.addOutputSlot();
            if (!cell.hasItem()) {
                continue;
            }
            slot.addItemStack(cell.stack());
            StageTooltipCallbacks.add(slot, cell.stack());
            List<Component> tooltip = cell.tooltip();
            if (!tooltip.isEmpty()) {
                slot.addRichTooltipCallback((view, builderTooltip) -> builderTooltip.addAll(tooltip));
            }
        }
    }

    /** 枪械页的弹药格：只画不参与查询，对着弹药按 U 应该去「弹药适用枪械」那一页。 */
    private void addAmmoSlot(IRecipeLayoutBuilder builder, TaczQueryRecipe recipe) {
        if (kind != TaczQueryRecipe.Kind.GUN_ATTACHMENTS || recipe.ammo().isEmpty()) {
            return;
        }
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.RENDER_ONLY,
                Layout.TACZ_AMMO_SLOT_X, Layout.TACZ_AMMO_SLOT_Y);
        for (ItemStack stack : recipe.ammo()) {
            slot.addItemStack(stack);
        }
        slot.setStandardSlotBackground();
        StageTooltipCallbacks.add(slot, recipe.ammo());
        slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(
                Component.translatable("eicore.anvil_guide.tacz.tooltip.ammo",
                        TextUtils.itemNames(recipe.ammo(), 4))));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, TaczQueryRecipe recipe, IFocusGroup focuses) {
        List<IRecipeSlotDrawable> slots = builder.getRecipeSlots().getSlots(RecipeIngredientRole.OUTPUT);
        Layout.placeScrollGrid(builder, slots);
    }

    @Override
    public void draw(TaczQueryRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        arrow.draw(guiGraphics, Layout.ARROW_X, Layout.ARROW_Y);

        Font font = Minecraft.getInstance().font;
        if (kind == TaczQueryRecipe.Kind.GUN_ATTACHMENTS && !recipe.ammo().isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("eicore.anvil_guide.tacz.ammo_label"),
                    Layout.TACZ_AMMO_TEXT_X, Layout.TACZ_AMMO_TEXT_Y, Layout.COLOR_TEXT_DIM, false);
        }
        guiGraphics.drawString(font, Component.translatable(kind.statsKey(), recipe.itemCount()),
                Layout.TEXT_X, Layout.STATS_TEXT_Y, Layout.COLOR_TEXT, false);
        guiGraphics.drawString(font, Component.translatable(kind.hintKey()),
                Layout.TEXT_X, Layout.HINT_TEXT_Y, Layout.COLOR_TEXT_DIM, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, TaczQueryRecipe recipe, IRecipeSlotsView recipeSlotsView,
                           double mouseX, double mouseY) {
        if (!TextUtils.isInside(mouseX, mouseY, Layout.TEXT_X, Layout.STATS_TEXT_Y - 2,
                Layout.WIDTH - Layout.TEXT_X * 2, Layout.TEXT_LINE_HEIGHT * 2 + 4)) {
            return;
        }
        tooltip.add(Component.translatable(kind.legendKey()));
    }
}
