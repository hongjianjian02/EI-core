package com.mysterious.mystic.foodbox;

import com.mysterious.mystic.MysteriousMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FlavorBoxComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MysteriousMod.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FlavorBoxContents>> CONTENTS =
            COMPONENTS.registerComponentType("flavor_box_contents", builder -> builder
                    .persistent(FlavorBoxContents.CODEC).networkSynchronized(FlavorBoxContents.STREAM_CODEC));

    private FlavorBoxComponents() {
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
