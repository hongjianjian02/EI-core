package com.mysterious.mystic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mysterious.mystic.compat.astages.EmiStageIndicators;
import dev.emi.emi.api.stack.EmiIngredient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.emi.emi.screen.EmiScreenManager", remap = false)
public abstract class EmiSidebarStageTooltipMixin {
    @WrapOperation(method = "renderCurrentTooltip(Ldev/emi/emi/runtime/EmiDrawContext;IIFLdev/emi/emi/screen/EmiScreenBase;)V",
            at = @At(value = "INVOKE",
            target = "Ldev/emi/emi/api/stack/EmiIngredient;getTooltip()Ljava/util/List;"))
    private static List<ClientTooltipComponent> eicore$addStageHints(EmiIngredient ingredient,
            Operation<List<ClientTooltipComponent>> original) {
        var lines = original.call(ingredient);
        var hints = EmiStageIndicators.itemHints(ingredient);
        if (hints.isEmpty()) return lines;

        var result = new ArrayList<>(lines);
        result.addAll(Math.min(1, result.size()), hints.stream().map(EmiStageIndicators::tooltipComponent).toList());
        return result;
    }
}
