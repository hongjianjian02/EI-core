package com.mysterious.mystic.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.alessandro.astages.infrastructure.integration.emi.EmiRecipeStagesPlugin", remap = false)
public abstract class AStagesEmiRecipeVisibilityMixin {
    @Inject(method = "hideRecipes", at = @At("HEAD"), cancellable = true)
    private void eicore$keepRecipesVisible(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}
