package com.mysterious.mystic.compat.anvilguide.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

/** 物品格子左上角的红色感叹号：提示这件物品与当前附魔书存在互斥附魔。 */
public final class ConflictBadge implements IDrawable {

    private static final int SIZE = 16;

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
        guiGraphics.fill(x, y, x + 6, y + 6, 0xE0A81A12);
        guiGraphics.fill(x, y, x + 6, y + 1, 0xFFFFD8CF);

        Font font = Minecraft.getInstance().font;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x + 1.0F, y, 0.0F);
        pose.scale(0.5F, 0.5F, 1.0F);
        guiGraphics.drawString(font, "!", 0, 0, 0xFFFFFFFF, true);
        pose.popPose();
    }
}
