package com.mysterious.mystic.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.alessandro.astages.infrastructure.integration.jei.JeiRecipeStagesPlugin", remap = false)
public abstract class AStagesJeiRecipeVisibilityMixin {
    @Inject(method = "updateRecipeGui", at = @At("HEAD"), cancellable = true)
    private static void eicore$keepRecipesVisible(CallbackInfo callback) {
        // Both the mod-wide and exact-recipe JEI hide/unhide passes run through this method.
        callback.cancel();
    }
}
