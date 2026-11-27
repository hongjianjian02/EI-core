package com.mysterious.mystic.compat.anvilguide.jei;

import com.mysterious.mystic.compat.anvilguide.util.TextUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

/**
 * 附魔书格子右下角的等级角标：只画罗马数字本身，不铺底色。
 *
 * <p>和原版物品数量一样用「白字 + 阴影」贴着格子右下角，
 * 数字比格子宽时按比例缩小，保证 XII 这类长等级也不会溢出。</p>
 */
public final class LevelBadge implements IDrawable {

    /** JEI 的槽位矩形就是 16×16 的物品区，18×18 的底框由 JEI 自己画在外面。 */
    private static final int SIZE = 16;
    private static final int LINE_HEIGHT = 9;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private final String text;

    public LevelBadge(int level) {
        this.text = TextUtils.roman(level);
    }

    @Override
    public int getWidth() {
        return SIZE;
    }

    @Override
    public int getHeight() {
        return SIZE;
    }

    @Override
    public void draw(GuiGraphics guiGraphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        int textWidth = Math.max(1, font.width(text));
        int available = SIZE - 1;
        float scale = textWidth <= available ? 1.0F : available / (float) textWidth;

        float drawX = x + SIZE - textWidth * scale;
        float drawY = y + SIZE - LINE_HEIGHT * scale;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        // 整个绘制处带 0.01 的偏移，避免缩放后的文字和物品图标在同一深度闪面
        pose.translate(drawX, drawY, 0.01F);
        pose.scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, TEXT_COLOR, true);
        pose.popPose();
    }
}
