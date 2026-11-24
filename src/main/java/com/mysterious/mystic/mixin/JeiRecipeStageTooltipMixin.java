package com.mysterious.mystic.mixin;

import com.mysterious.mystic.compat.astages.ClientRecipeStageTooltips;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.library.gui.recipes.RecipeLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mezz.jei.library.gui.recipes.RecipeLayout", remap = false)
public abstract class JeiRecipeStageTooltipMixin {
    @Inject(method = "ensureRecipeExtrasAreCreated", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lmezz/jei/api/recipe/category/IRecipeCategory;createRecipeExtras(Lmezz/jei/api/gui/widgets/IRecipeExtrasBuilder;Ljava/lang/Object;Lmezz/jei/api/recipe/IFocusGroup;)V"))
    @SuppressWarnings("removal") // 19.76 exposes only the legacy callback on an already built slot.
    private void eicore$attachOutputTooltips(CallbackInfo callback) {
        var layout = (RecipeLayout<?>) (Object) this;
        RecipeHolder<?> recipe = eicore$backingRecipe(layout);
        if (recipe == null) return;

        for (var slot : layout.getRecipeSlots().getSlots(RecipeIngredientRole.OUTPUT)) {
            slot.addTooltipCallback((view, lines) -> lines.addAll(ClientRecipeStageTooltips.forRecipe(recipe)));
        }
    }

    private static RecipeHolder<?> eicore$backingRecipe(IRecipeLayoutDrawable<?> layout) {
        Object displayed = layout.getRecipe();
        if (displayed instanceof RecipeHolder<?> holder) return holder;
        var level = Minecraft.getInstance().level;
        if (!(displayed instanceof Recipe<?>) || level == null) return null;
        var ids = layout.getSourceRecipeIds();
        if (ids.size() != 1) return null;
        return level.getRecipeManager().byKey(ids.getFirst())
                .filter(holder -> holder.value() == displayed).orElse(null);
    }
}
