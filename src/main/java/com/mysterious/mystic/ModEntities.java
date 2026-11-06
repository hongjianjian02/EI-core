package com.mysterious.mystic;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MysteriousMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<VoidHerrscher>> VOID_HERRSCHER =
            ENTITY_TYPES.register("void_herrscher", () -> EntityType.Builder
                    .of(VoidHerrscher::new, MobCategory.MONSTER)
                    .sized(1.4F, 3.2F)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build("eicore:void_herrscher"));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener(ModEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(VOID_HERRSCHER.get(), VoidHerrscher.createAttributes().build());
    }
}
