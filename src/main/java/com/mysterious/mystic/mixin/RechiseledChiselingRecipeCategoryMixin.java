package com.mysterious.mystic.mixin;

import dev.emi.emi.jemi.impl.JemiRecipeLayoutBuilder;
import dev.emi.emi.jemi.impl.JemiRecipeSlotBuilder;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.supermartijn642.rechiseled.compat.jei.ChiselingRecipeCategory", remap = false)
public abstract class RechiseledChiselingRecipeCategoryMixin {
    @Inject(method = "setRecipe", at = @At("TAIL"))
    private void eicore$positionEmiSlots(IRecipeLayoutBuilder builder, @Coerce Object recipe, IFocusGroup focuses, CallbackInfo callback) {
        if (!(builder instanceof JemiRecipeLayoutBuilder emiBuilder)) {
            return;
        }

        int outputIndex = 0;
        for (JemiRecipeSlotBuilder slot : emiBuilder.slots) {
            if (slot.acceptor.role == RecipeIngredientRole.INPUT) {
                slot.x = 73;
                slot.y = 1;
            } else if (slot.acceptor.role == RecipeIngredientRole.OUTPUT) {
                slot.x = 1 + outputIndex % 9 * 18;
                slot.y = 22 + outputIndex / 9 * 18;
                outputIndex++;
            }
        }
    }

    @Inject(method = "createRecipeExtras", at = @At("HEAD"), cancellable = true)
    private void eicore$skipJeiOnlyExtrasInEmi(IRecipeExtrasBuilder builder, @Coerce Object recipe, IFocusGroup focuses, CallbackInfo callback) {
        if (builder.getRecipeSlots() == null) {
            callback.cancel();
        }
    }
}
