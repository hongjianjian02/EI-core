package com.mysterious.mystic.mixin;

import com.mysterious.mystic.compat.astages.EmiStageIndicators;
import dev.emi.emi.api.widget.SlotWidget;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.emi.emi.api.widget.SlotWidget", remap = false)
public abstract class EmiRecipeStageTooltipMixin {
    // Both native EMI slots and JEMI output slots call this when their tooltip is built.
    @Inject(method = "addSlotTooltip(Ljava/util/List;)V", at = @At("TAIL"))
    private void eicore$appendRecipeStages(List<ClientTooltipComponent> tooltip, CallbackInfo callback) {
        for (var hint : EmiStageIndicators.resultHints((SlotWidget) (Object) this)) {
            tooltip.add(EmiStageIndicators.tooltipComponent(hint));
        }
    }
}
