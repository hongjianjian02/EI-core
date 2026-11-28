package com.mysterious.mystic.mixin;

import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.jemi.JemiRecipe;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.emi.emi.jemi.JemiRecipe", remap = false)
public abstract class JemiRecipeArrowMixin {
    @Shadow public IRecipeCategory<?> category;

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void eicore$addRechiseledArrow(WidgetHolder widgets, CallbackInfo callback) {
        if (this.category == null || !this.category.getRecipeType().getUid().toString().equals("rechiseled:chiseling")) {
            return;
        }

        ResourceLocation arrow = ResourceLocation.fromNamespaceAndPath("rechiseled", "textures/screen/curved_arrow.png");
        widgets.addTexture(arrow, 89, 0, 20, 20, 0, 0);
    }
}
