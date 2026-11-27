package com.mysterious.mystic.compat.anvilguide.jei;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IRecipeLookup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.library.plugins.vanilla.anvil.AnvilRecipe;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeJeiAnvilReplacementTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void hidesOnlyJeisNativeAnvilRecipesAndRestoresThemWhenReplacementIsUnavailable() {
        IJeiAnvilRecipe nativeRecipe = new AnvilRecipe(
                List.of(new ItemStack(Items.IRON_SWORD)), List.of(new ItemStack(Items.IRON_INGOT)),
                List.of(new ItemStack(Items.IRON_SWORD)), ResourceLocation.withDefaultNamespace("test/native_anvil"));
        IJeiAnvilRecipe otherModRecipe = new IJeiAnvilRecipe() {
            @Override
            public List<ItemStack> getLeftInputs() {
                return List.of();
            }

            @Override
            public List<ItemStack> getRightInputs() {
                return List.of();
            }

            @Override
            public List<ItemStack> getOutputs() {
                return List.of();
            }

            @Override
            public ResourceLocation getUid() {
                return ResourceLocation.withDefaultNamespace("test/other_mod");
            }
        };
        ManagerCalls calls = new ManagerCalls();
        NativeJeiAnvilReplacement replacement = new NativeJeiAnvilReplacement();

        replacement.update(manager(List.of(nativeRecipe, otherModRecipe), calls), true);

        assertEquals(List.of(nativeRecipe), calls.hidden);
        assertFalse(calls.hidden.contains(otherModRecipe));

        replacement.update(manager(List.of(nativeRecipe, otherModRecipe), calls), false);

        assertEquals(List.of(nativeRecipe), calls.restored);
        assertFalse(calls.hidden.contains(otherModRecipe));
    }

    @Test
    void replacementUnavailableDoesNotHideTheNativeAnvilPage() {
        IJeiAnvilRecipe nativeRecipe = new AnvilRecipe(
                List.of(), List.of(), List.of(), ResourceLocation.withDefaultNamespace("test/native_anvil"));
        ManagerCalls calls = new ManagerCalls();

        new NativeJeiAnvilReplacement().update(manager(List.of(nativeRecipe), calls), false);

        assertTrue(calls.hidden.isEmpty());
        assertTrue(calls.restored.isEmpty());
    }

    @Test
    void nativeDetectionUsesJeisExactImplementationClass() {
        assertTrue(NativeJeiAnvilReplacement.isNativeRecipeClassName(
                "mezz.jei.library.plugins.vanilla.anvil.AnvilRecipe"));
        assertFalse(NativeJeiAnvilReplacement.isNativeRecipeClassName("other.mod.jei.AnvilRecipe"));
    }

    private static IRecipeManager manager(List<IJeiAnvilRecipe> recipes, ManagerCalls calls) {
        return (IRecipeManager) Proxy.newProxyInstance(IRecipeManager.class.getClassLoader(),
                new Class<?>[] {IRecipeManager.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "createRecipeLookup" -> lookup(recipes);
                    case "hideRecipes" -> {
                        if (arguments[0] == RecipeTypes.ANVIL) {
                            calls.hidden.addAll((List<?>) arguments[1]);
                        }
                        yield null;
                    }
                    case "unhideRecipes" -> {
                        if (arguments[0] == RecipeTypes.ANVIL) {
                            calls.restored.addAll((List<?>) arguments[1]);
                        }
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static IRecipeLookup<IJeiAnvilRecipe> lookup(List<IJeiAnvilRecipe> recipes) {
        return (IRecipeLookup<IJeiAnvilRecipe>) Proxy.newProxyInstance(IRecipeLookup.class.getClassLoader(),
                new Class<?>[] {IRecipeLookup.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("get")) {
                        return recipes.stream();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class ManagerCalls {
        private final List<Object> hidden = new ArrayList<>();
        private final List<Object> restored = new ArrayList<>();
    }
}
