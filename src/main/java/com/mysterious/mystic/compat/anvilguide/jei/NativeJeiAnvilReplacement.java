package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.List;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;

/** Replaces only the built-in JEI anvil recipe implementation after all guide pages are available. */
final class NativeJeiAnvilReplacement {

    private static final String JEI_NATIVE_RECIPE_CLASS =
            "mezz.jei.library.plugins.vanilla.anvil.AnvilRecipe";

    private List<IJeiAnvilRecipe> hiddenRecipes = List.of();

    void update(IRecipeManager manager, boolean replacementAvailable) {
        restore(manager);
        if (!replacementAvailable) {
            return;
        }

        List<IJeiAnvilRecipe> recipes = manager.createRecipeLookup(RecipeTypes.ANVIL).get()
                .filter(NativeJeiAnvilReplacement::isNativeRecipe)
                .toList();
        if (recipes.isEmpty()) {
            return;
        }

        // Keep the exact objects before calling JEI so a partial failure can still be undone.
        hiddenRecipes = List.copyOf(recipes);
        try {
            manager.hideRecipes(RecipeTypes.ANVIL, hiddenRecipes);
        } catch (RuntimeException | LinkageError exception) {
            try {
                restore(manager);
            } catch (RuntimeException | LinkageError restoreException) {
                exception.addSuppressed(restoreException);
            }
            throw exception;
        }
    }

    void restore(IRecipeManager manager) {
        if (hiddenRecipes.isEmpty()) {
            return;
        }
        manager.unhideRecipes(RecipeTypes.ANVIL, hiddenRecipes);
        hiddenRecipes = List.of();
    }

    static boolean isNativeRecipe(IJeiAnvilRecipe recipe) {
        return recipe != null && isNativeRecipeClassName(recipe.getClass().getName());
    }

    static boolean isNativeRecipeClassName(String className) {
        return JEI_NATIVE_RECIPE_CLASS.equals(className);
    }
}
