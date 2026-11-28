package com.mysterious.mystic.mixin;

import com.supermartijn642.rechiseled.api.chiseling.ChiselingBlockShape;
import com.supermartijn642.rechiseled.api.chiseling.ChiselingEntry;
import com.supermartijn642.rechiseled.api.chiseling.ChiselingRecipe;
import mezz.jei.api.recipe.category.IRecipeCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.emi.emi.jemi.JemiRecipe", remap = false)
public abstract class JemiRecipeDisplayHeightMixin {
    @Shadow public Object recipe;
    @Shadow public IRecipeCategory<?> category;

    @Inject(method = "getDisplayHeight", at = @At("HEAD"), cancellable = true)
    private void eicore$fitRechiseledOutputs(CallbackInfoReturnable<Integer> callback) {
        if (this.category == null || this.recipe == null
            || !this.category.getRecipeType().getUid().toString().equals("rechiseled:chiseling")
            || !(this.recipe instanceof ChiselingRecipe chiselingRecipe)) {
            return;
        }

        int outputSlots = 0;
        for (ChiselingBlockShape shape : ChiselingBlockShape.values()) {
            for (ChiselingEntry entry : chiselingRecipe.entries()) {
                if (entry.hasRegularItem(shape) || entry.hasConnectingItem(shape)) {
                    outputSlots++;
                }
            }
        }

        int outputRows = (outputSlots + 8) / 9;
        callback.setReturnValue(Math.max(this.category.getHeight(), 22 + outputRows * 18));
    }
}
