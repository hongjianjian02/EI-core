package com.mysterious.mystic;

import com.mysterious.mystic.entity.ElectricEelEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MysteriousMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ElectricEelEntity>> ELECTRIC_EEL =
            ENTITIES.register("electric_eel", () -> EntityType.Builder
                    .of(ElectricEelEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.6F, 0.4F)
                    .clientTrackingRange(8)
                    .build(MysteriousMod.MODID + ":electric_eel"));

    private ModEntities() {
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::createAttributes);
        bus.addListener(ModEntities::registerSpawnPlacements);
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(ELECTRIC_EEL.get(), ElectricEelEntity.createAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ELECTRIC_EEL.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ElectricEelEntity::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
