package com.mysterious.mystic.compat.anvilguide.emi;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;

/** Hide the locked EMI anvil lists only after repair grids have been registered. */
final class NativeAnvilRepairReplacement {
    static final ResourceLocation CATEGORY_ID = ResourceLocation.fromNamespaceAndPath("emi", "anvil_repairing");
    private static final Map<String, String> NATIVE_TYPES = Map.of(
            "dev.emi.emi.recipe.EmiAnvilRecipe", "/anvil/repairing/material/",
            "dev.emi.emi.recipe.special.EmiAnvilRepairItemRecipe", "/anvil/repairing/tool/",
            "dev.emi.emi.recipe.special.EmiAnvilEnchantRecipe", "/anvil/enchanting/");
    private final boolean registeredRepairGrids;

    NativeAnvilRepairReplacement(int registeredRepairPages) {
        registeredRepairGrids = registeredRepairPages > 0;
    }

    boolean isNativeType(String className) {
        return className != null && NATIVE_TYPES.containsKey(className);
    }

    boolean shouldHide(String className, ResourceLocation categoryId, ResourceLocation recipeId) {
        if (!registeredRepairGrids || !isNativeType(className) || !CATEGORY_ID.equals(categoryId)
                || recipeId == null || !"emi".equals(recipeId.getNamespace())) {
            return false;
        }
        return recipeId.getPath().startsWith(NATIVE_TYPES.get(className));
    }
}
