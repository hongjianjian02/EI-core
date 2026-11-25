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
        // Commit queued icons before this immediate overlay. Tooltips render afterwards;
        // the badge must not leave depth that can hide their background or text.
        var renderState = new GlStateBackup();
        RenderSystem.backupGlState(renderState);
        try {
            graphics.flush();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            graphics.blit(UNKNOWN_ITEM_TEXTURE, iconX + 7, iconY, 9, 9, 0, 0, 16, 16, 16, 16);
        } finally {
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
