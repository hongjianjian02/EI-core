package com.mysterious.mystic.scribe;

import com.mysterious.mystic.MysteriousMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RitualScribe {
    public static final ResourceLocation UPGRADE_ID = ResourceLocation.fromNamespaceAndPath(MysteriousMod.MODID, "ritual_scribe_upgrade");
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MysteriousMod.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MysteriousMod.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MysteriousMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ScribeState>> STATE =
            COMPONENTS.registerComponentType("ritual_scribe_state", builder -> builder
                    .persistent(ScribeState.CODEC).networkSynchronized(ScribeState.STREAM_CODEC));
    public static final DeferredHolder<MenuType<?>, MenuType<RitualScribeMenu>> MENU =
            MENUS.register("ritual_scribe", () -> new MenuType<>(RitualScribeMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RitualScribeUpgradeRecipe>> UPGRADE =
            RECIPES.register(UPGRADE_ID.getPath(), () -> new SimpleCraftingRecipeSerializer<>(RitualScribeUpgradeRecipe::new));

    private RitualScribe() {
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        MENUS.register(bus);
        RECIPES.register(bus);
    }
}
