package com.mysterious.mystic;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen {
    private static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, MysteriousMod.MODID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<StarlightDimensionFilter>>
            STARLIGHT_DIMENSION = PLACEMENT_MODIFIERS.register("starlight_dimension",
                    () -> () -> StarlightDimensionFilter.CODEC);

    private ModWorldgen() {
    }

    public static void register(IEventBus bus) {
        PLACEMENT_MODIFIERS.register(bus);
    }
}
