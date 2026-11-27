package com.mysterious.mystic.compat.anvilguide;

import net.minecraft.resources.ResourceLocation;

/** Shared identity only; browser discovery owns all optional client entrypoints. */
public final class AnvilGuide {
    public static final String MOD_ID = "eicore";

    private AnvilGuide() {
    }

    public static ResourceLocation id(String path) {
        // EMI treats slash-prefixed recipe IDs as synthetic; retain that convention.
        return ResourceLocation.fromNamespaceAndPath(MOD_ID,
                path.startsWith("/") ? "/anvil_guide" + path : "anvil_guide/" + path);
    }
}
