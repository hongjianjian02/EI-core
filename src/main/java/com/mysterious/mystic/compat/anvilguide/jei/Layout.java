package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.List;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IScrollGridWidget;
import mezz.jei.api.gui.widgets.ScrollbarVisibility;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 聚合查询页签共用的界面几何与面板绘制。
 *
 * <p>左侧是「输入槽 + 箭头 + 折叠按钮」的窄列，右侧是 6 列 × 4 行的滚动网格：
 * 网格由 JEI 的滚动控件负责格位、物品与滚动条，这里只提供尺寸并把面板画在它后面。</p>
 */
public final class Layout {

    public static final int WIDTH = 184;
    /** 物品页：底部两行字（统计 + 提示），再加一行铁砧修复材料。 */
    public static final int ITEM_HEIGHT = 124;
    /** 附魔书页：装备类型图例可能折成两行，再留一行提示。 */
    public static final int BOOK_HEIGHT = 114;
    /** 奥术铁砧页：只有输入槽 + 卷轴网格 + 两行文字，没有修复材料那一行。 */
    public static final int ARCANE_HEIGHT = 106;
    /** 铁砧修复页：和奥术铁砧页一样只有输入槽 + 物品网格 + 两行文字。 */
    public static final int REPAIR_HEIGHT = 106;
    /** 枪械 / 配件查询页：输入槽 + 网格 + 两行文字，枪械页多一格弹药。 */
    public static final int TACZ_HEIGHT = 106;

    /** 网格列数，横向固定 6 格。 */
    public static final int COLUMNS = 6;
    /** 网格可见行数，多出来的内容靠滚动条查看。 */
    public static final int ROWS = 4;
    public static final int SLOT_SIZE = 18;

    /** 滚动网格内容区左上角（不含右侧滚动条）。 */
    public static final int GRID_X = 54;
    public static final int GRID_Y = 6;
    public static final int GRID_WIDTH = COLUMNS * SLOT_SIZE;
    public static final int GRID_HEIGHT = ROWS * SLOT_SIZE;

    public static final int INPUT_X = 6;
    public static final int INPUT_Y = 20;

    public static final int ARROW_X = 28;
    public static final int ARROW_Y = 21;

    public static final int TOGGLE_X = 5;
    public static final int TOGGLE_Y = 44;
    public static final int TOGGLE_WIDTH = 44;
    public static final int TOGGLE_HEIGHT = 14;

    public static final int TEXT_X = 6;
    public static final int STATS_TEXT_Y = 82;
    public static final int HINT_TEXT_Y = 92;
    /** 铁砧修复材料：一格材料槽 + 右侧的说明文字。 */
    public static final int REPAIR_SLOT_X = 6;
    public static final int REPAIR_SLOT_Y = 102;
    public static final int REPAIR_TEXT_X = REPAIR_SLOT_X + SLOT_SIZE + 3;
    public static final int REPAIR_TEXT_Y = 107;
    /** 枪械配件页：枪械格正下方那一格弹药，以及右侧的「弹药」小字。 */
    public static final int TACZ_AMMO_SLOT_X = 6;
    public static final int TACZ_AMMO_SLOT_Y = 44;
    public static final int TACZ_AMMO_TEXT_X = TACZ_AMMO_SLOT_X + SLOT_SIZE + 2;
    public static final int TACZ_AMMO_TEXT_Y = 48;
    /** 底部文字的行距。 */
    public static final int TEXT_LINE_HEIGHT = 10;

    public static final int COLOR_GRID_FILL = 0xFFF1ECFB;
    public static final int COLOR_GRID_BORDER = 0xFFC6B8E6;
    public static final int COLOR_BUTTON_FILL = 0xFFE8DFFA;
    public static final int COLOR_BUTTON_FILL_HOVER = 0xFFD6C8F4;
    public static final int COLOR_BUTTON_BORDER = 0xFF9F8DCC;
    public static final int COLOR_BUTTON_TEXT = 0xFF3B2F5E;
    public static final int COLOR_TEXT = 0xFF3A3054;
    public static final int COLOR_TEXT_DIM = 0xFF6C6486;
    /** 诅咒附魔分隔带的标签与细线。 */
    public static final int COLOR_CURSE_TEXT = 0xFF9A3A5A;
    /** 法术卷轴分隔带的标签。 */
    public static final int COLOR_SCROLL_TEXT = 0xFF2F5E8C;
    /** 枪械 / 配件查询页分隔带（枪种、配件位置）的标签。 */
    public static final int COLOR_TACZ_SECTION = 0xFF2E6B5A;
    public static final int COLOR_DIVIDER = 0xFFBDAEDB;

    /** 网格面板比内容区四周各大 3 像素。 */
    private static final int PANEL_PADDING = 3;

    private Layout() {
    }

    public static int gridAreaX() {
        return GRID_X - PANEL_PADDING;
    }

    public static int gridAreaY() {
        return GRID_Y - PANEL_PADDING;
    }

    /** 面板宽度要连右侧滚动条一起包住，滚动条宽度由 JEI 的 helper 给出。 */
    public static int gridAreaWidth(int scrollbarWidth) {
        return GRID_WIDTH + scrollbarWidth + PANEL_PADDING * 2;
    }

    public static int gridAreaHeight() {
        return GRID_HEIGHT + PANEL_PADDING * 2;
    }

    /** 命名的查询锚点、修复材料留在左侧，只把未命名展示槽交给网格。 */
    public static void placeRenderOnlyScrollGrid(IRecipeExtrasBuilder builder) {
        placeScrollGrid(builder, builder.getRecipeSlots().getSlots(RecipeIngredientRole.RENDER_ONLY)
                .stream().filter(slot -> slot.getSlotName().isEmpty()).toList());
    }

    /**
     * 把 JEI 的滚动网格摆到面板里的网格区。
     *
     * <p>背景先于网格注册，绘制时直接读取该网格的真实宽高。
     * 不依赖旧滚动条宽度接口或分类对象上的共享状态，每张配方的滚动区域独立。</p>
     */
    public static void placeScrollGrid(IRecipeExtrasBuilder builder, List<IRecipeSlotDrawable> slots) {
        if (slots.isEmpty()) {
            return;
        }
        GridPanel panel = new GridPanel();
        builder.addDrawableWidget(panel).setPosition(gridAreaX(), gridAreaY());
        panel.grid = builder.addScrollGridWidget(slots, COLUMNS, ROWS)
                .setScrollbarVisibility(ScrollbarVisibility.AUTO_HIDE)
                .setPosition(GRID_X, GRID_Y);
    }

    private static final class GridPanel implements IDrawable {
        private IScrollGridWidget grid;

        @Override
        public int getWidth() {
            return (grid == null ? GRID_WIDTH : grid.getWidth()) + PANEL_PADDING * 2;
        }

        @Override
        public int getHeight() {
            return (grid == null ? GRID_HEIGHT : grid.getHeight()) + PANEL_PADDING * 2;
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y) {
            panel(graphics, x, y, getWidth(), getHeight(), COLOR_GRID_FILL, COLOR_GRID_BORDER);
        }
    }

    /** 圆角 1 像素的面板：先填底色再描一圈边框。 */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int fill, int border) {
        graphics.fill(x + 1, y, x + width - 1, y + height, fill);
        graphics.fill(x, y + 1, x + width, y + height - 1, fill);
        graphics.fill(x + 1, y, x + width - 1, y + 1, border);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, border);
        graphics.fill(x, y + 1, x + 1, y + height - 1, border);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, border);
    }

    public static void button(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered) {
        panel(graphics, x, y, width, height,
                hovered ? COLOR_BUTTON_FILL_HOVER : COLOR_BUTTON_FILL, COLOR_BUTTON_BORDER);
    }
}
