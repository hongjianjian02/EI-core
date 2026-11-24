package com.mysterious.mystic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mysterious.mystic.compat.astages.EmiStageIndicators;
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
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Ldev/emi/emi/api/widget/Widget;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void eicore$drawRecipeBadge(Widget widget, GuiGraphics graphics, int mouseX, int mouseY,
                                        float delta, Operation<Void> original) {
        original.call(widget, graphics, mouseX, mouseY, delta);
        if (widget instanceof SlotWidget slot && !EmiStageIndicators.resultHints(slot).isEmpty()) {
            var bounds = slot.getBounds();
            EmiStageIndicators.drawBadge(graphics, bounds.x() + bounds.width() - 1, bounds.y() + 1);
        }
    }
}
