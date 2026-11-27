package com.mysterious.mystic.compat.anvilguide.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 段落上方的分隔带：一整行「细线 + 标签」，把诅咒附魔、法术卷轴和前面的内容隔开。
 *
 * <p>JEI 的滚动网格里每个格子只能挂一个 overlay，所以这条带子挂在预留那一行的最后一格上，
 * 往左铺满整行，把这一行空占位格的槽位底图盖掉再画线。
 * 跟着格子一起滚动，所以不会出现位置对不上的残影。</p>
 *
 * <p>坐标是网格内部的局部坐标：{@code (x, y)} 是 18×18 格子里 16×16 物品区的左上角。</p>
 */
public final class SectionHeader implements IDrawable {

    /** 分隔带高度：正好一行。 */
    private static final int ROW = Layout.SLOT_SIZE;
    private static final int TEXT_HEIGHT = 8;

    private final Component label;
    private final int color;

    public SectionHeader(Component label) {
        this(label, Layout.COLOR_CURSE_TEXT);
    }

    public SectionHeader(Component label, int color) {
        this.label = label;
        this.color = color;
    }

    @Override
    public int getWidth() {
        return ROW;
    }

    @Override
    public int getHeight() {
        return ROW;
    }

    @Override
    public void draw(GuiGraphics guiGraphics, int x, int y) {
        // 挂在最后一列上，往左铺满整行
        int left = x - 1 - (Layout.COLUMNS - 1) * ROW;
        int top = y - 1;
        int bottom = top + ROW;
        int visibleTop = Math.max(top, 0);
        int visibleBottom = Math.min(bottom, Layout.GRID_HEIGHT);
        if (visibleBottom <= visibleTop) {
            return;
        }
        // 先用面板底色盖掉这一行占位格的槽位底图
        guiGraphics.fill(left, visibleTop, left + Layout.GRID_WIDTH, visibleBottom, Layout.COLOR_GRID_FILL);
        if (visibleTop > top || visibleBottom < bottom) {
            // 滚动到只露一半时只铺底色，免得文字被切掉一半
            return;
        }

        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(label);
        int textX = left + (Layout.GRID_WIDTH - textWidth) / 2;
        int textY = top + (ROW - TEXT_HEIGHT) / 2;
        if (textWidth > Layout.GRID_WIDTH - 12) {
            guiGraphics.drawString(font, label, left + 2, textY, color, false);
            return;
        }
        // 标签两侧各一段细线，横穿整行
        int lineY = top + ROW / 2;
        guiGraphics.fill(left + 3, lineY, textX - 4, lineY + 1, Layout.COLOR_DIVIDER);
        guiGraphics.fill(textX + textWidth + 4, lineY, left + Layout.GRID_WIDTH - 3, lineY + 1,
                Layout.COLOR_DIVIDER);
        guiGraphics.drawString(font, label, textX, textY, color, false);
    }
}
