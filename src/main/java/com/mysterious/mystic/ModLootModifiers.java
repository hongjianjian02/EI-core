package com.mysterious.mystic;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MysteriousMod.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<TreehouseSeedsLootModifier>>
            TREEHOUSE_SEEDS = LOOT_MODIFIERS.register("treehouse_seeds", () -> TreehouseSeedsLootModifier.CODEC);

    private ModLootModifiers() {
    }

    public static void register(IEventBus bus) {
        LOOT_MODIFIERS.register(bus);
    }
}
