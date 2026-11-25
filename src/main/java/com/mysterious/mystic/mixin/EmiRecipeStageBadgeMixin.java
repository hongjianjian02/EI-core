package com.mysterious.mystic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mysterious.mystic.compat.astages.EmiStageIndicators;
import com.mysterious.mystic.compat.astages.StageBadgeRenderer;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.emi.emi.screen.RecipeScreen", remap = false)
public abstract class EmiRecipeStageBadgeMixin {
    // Goes through Widget.render so it also covers JEI categories displayed via JEMI.
    @WrapOperation(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At(value = "INVOKE",
            target = "Ldev/emi/emi/api/widget/Widget;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void eicore$drawRecipeBadge(Widget widget, GuiGraphics graphics, int mouseX, int mouseY,
                                        float delta, Operation<Void> original) {
        if (!(widget instanceof SlotWidget slot)) {
            original.call(widget, graphics, mouseX, mouseY, delta);
            return;
        }
        StageBadgeRenderer.withoutItemBadges(() -> original.call(widget, graphics, mouseX, mouseY, delta));
        EmiStageIndicators.drawResultBadge(graphics, slot);
    }
}
