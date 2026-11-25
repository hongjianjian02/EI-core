package com.mysterious.mystic.compat.astages;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.GlStateBackup;

public final class StageBadgeRenderer {
    private static final ResourceLocation UNKNOWN_ITEM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "hardcoreastagesitem", "textures/item/unknown_item.png");
    private static int suppressionDepth;

    private StageBadgeRenderer() {
    }

    public static void draw(GuiGraphics graphics, int iconX, int iconY) {
        // EMI's batch renderer may leave blending disabled; preserve the caller's state.
        var renderState = new GlStateBackup();
        RenderSystem.backupGlState(renderState);
        graphics.pose().pushPose();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.pose().translate(0, 0, 300);
            graphics.blit(UNKNOWN_ITEM_TEXTURE, iconX + 7, iconY, 9, 9, 0, 0, 16, 16, 16, 16);
        } finally {
            graphics.pose().popPose();
            RenderSystem.restoreGlState(renderState);
        }
    }

    public static void withoutItemBadges(Runnable render) {
        suppressionDepth++;
        try {
            render.run();
        } finally {
            suppressionDepth--;
        }
    }

    public static boolean itemBadgesSuppressed() {
        return suppressionDepth > 0;
    }
}
