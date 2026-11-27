package com.mysterious.mystic.compat.anvilguide.emi;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Registration readiness and exact native identities, without loading EMI client classes. */
class NativeAnvilRepairReplacementTest {
    private static final ResourceLocation CATEGORY = id("emi", "anvil_repairing");
    private static final List<NativePage> NATIVE_PAGES = List.of(
            new NativePage("dev.emi.emi.recipe.EmiAnvilRecipe", "/anvil/repairing/material/iron_sword"),
            new NativePage("dev.emi.emi.recipe.special.EmiAnvilRepairItemRecipe", "/anvil/repairing/tool/iron_sword"),
            new NativePage("dev.emi.emi.recipe.special.EmiAnvilEnchantRecipe", "/anvil/enchanting/iron_sword"));

    @Test
    void aRegisteredRepairGridHidesAllThreeExactNativeLists() {
        NativeAnvilRepairReplacement replacement = new NativeAnvilRepairReplacement(1);
        for (NativePage page : NATIVE_PAGES) {
            assertTrue(replacement.isNativeType(page.className()));
            assertTrue(replacement.shouldHide(page.className(), CATEGORY, page.id()), page.className());
        }
    }

    @Test
    void noRegisteredRepairGridPreservesAllNativeLists() {
        for (int count : List.of(0, -1)) {
            NativeAnvilRepairReplacement replacement = new NativeAnvilRepairReplacement(count);
            for (NativePage page : NATIVE_PAGES) {
                assertFalse(replacement.shouldHide(page.className(), CATEGORY, page.id()), page.className());
            }
        }
    }

    @Test
    void eachClassRequiresItsOwnPrefixRatherThanAnyAnvilId() {
        NativeAnvilRepairReplacement replacement = new NativeAnvilRepairReplacement(111);
        for (NativePage type : NATIVE_PAGES) {
            for (NativePage path : NATIVE_PAGES) {
                assertEquals(type == path, replacement.shouldHide(type.className(), CATEGORY, path.id()));
            }
            for (String path : List.of("/anvil/repairing/material_extra/iron_sword",
                    "/anvil/repairing/tool_extra/iron_sword", "/anvil/enchanting_extra/iron_sword",
                    "anvil/repairing/material/iron_sword", "/anvil/query/iron_sword")) {
                assertFalse(replacement.shouldHide(type.className(), CATEGORY, id("emi", path)), path);
            }
        }
    }

    @Test
    void differentCategoriesNamespacesAndMissingIdsArePreserved() {
        NativeAnvilRepairReplacement replacement = new NativeAnvilRepairReplacement(111);
        for (NativePage page : NATIVE_PAGES) {
            for (ResourceLocation category : List.of(id("emi", "grindstone"),
                    id("other", "anvil_repairing"), id("emi", "anvil_enchanting"))) {
                assertFalse(replacement.shouldHide(page.className(), category, page.id()), category.toString());
            }
            assertFalse(replacement.shouldHide(page.className(), null, page.id()));
            assertFalse(replacement.shouldHide(page.className(), CATEGORY, null));
            assertFalse(replacement.shouldHide(page.className(), CATEGORY, id("other", page.path())));
        }
    }

    @Test
    void thirdPartySubclassesGridBridgeAndMachineTypesArePreserved() {
        NativeAnvilRepairReplacement replacement = new NativeAnvilRepairReplacement(111);
        for (String type : List.of("other.mod.EmiAnvilRecipe", "dev.emi.emi.recipe.EmiAnvilRecipeSubclass",
                "dev.emi.emi.recipe.EmiGrindstoneRecipe", "dev.emi.emi.jemi.JemiRecipe",
                "com.mysterious.mystic.compat.anvilguide.emi.EmiAnvilRepairRecipe")) {
            assertFalse(replacement.isNativeType(type), type);
            for (NativePage page : NATIVE_PAGES) {
                assertFalse(replacement.shouldHide(type, CATEGORY, page.id()), type);
            }
        }
        assertFalse(replacement.isNativeType(null));
        assertFalse(replacement.shouldHide(null, CATEGORY, NATIVE_PAGES.getFirst().id()));
    }

    @Test
    void aNewEmptyReloadDoesNotInheritAnEarlierSuccessfulReplacement() {
        NativeAnvilRepairReplacement successful = new NativeAnvilRepairReplacement(111);
        NativeAnvilRepairReplacement failedReload = new NativeAnvilRepairReplacement(0);
        NativeAnvilRepairReplacement nextWorld = new NativeAnvilRepairReplacement(1);
        for (NativePage page : NATIVE_PAGES) {
            assertTrue(successful.shouldHide(page.className(), CATEGORY, page.id()));
            assertFalse(failedReload.shouldHide(page.className(), CATEGORY, page.id()));
            assertTrue(nextWorld.shouldHide(page.className(), CATEGORY, page.id()));
        }
    }

    private record NativePage(String className, String path) {
        ResourceLocation id() {
            return NativeAnvilRepairReplacementTest.id("emi", path);
        }
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
