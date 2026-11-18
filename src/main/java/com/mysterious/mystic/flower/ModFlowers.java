package com.mysterious.mystic.flower;

import com.mysterious.mystic.MysteriousMod;
import java.util.EnumMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import vazkii.botania.api.block.WandBindable;
import vazkii.botania.api.neoforge.BotaniaNeoForgeCapabilities;
import vazkii.botania.common.block.flower.SpecialFlowerBlock;

public final class ModFlowers {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MysteriousMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MysteriousMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MysteriousMod.MODID);
    public static final EnumMap<FlowerKind, DeferredBlock<SpecialFlowerBlock>> BLOCKS_BY_KIND =
            new EnumMap<>(FlowerKind.class);
    public static final EnumMap<FlowerKind, DeferredItem<GeneratingFlowerItem>> ITEMS_BY_KIND =
            new EnumMap<>(FlowerKind.class);
    public static final EnumMap<FlowerKind, DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratingFlowerEntity>>>
            TYPES_BY_KIND = new EnumMap<>(FlowerKind.class);

    static {
        for (FlowerKind kind : FlowerKind.values()) {
            BLOCKS_BY_KIND.put(kind, BLOCKS.register(kind.id, () -> new SpecialFlowerBlock(
                    MobEffects.SATURATION, 1, BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY),
                    () -> TYPES_BY_KIND.get(kind).get())));
            ITEMS_BY_KIND.put(kind, ITEMS.registerItem(kind.id,
                    properties -> new GeneratingFlowerItem(BLOCKS_BY_KIND.get(kind).get(), properties, kind)));
            TYPES_BY_KIND.put(kind, TYPES.register(kind.id, () -> BlockEntityType.Builder.of(
                    (pos, state) -> new GeneratingFlowerEntity(kind, pos, state),
                    BLOCKS_BY_KIND.get(kind).get()).build(null)));
        }
    }

    private ModFlowers() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
        bus.addListener(ModFlowers::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        TYPES_BY_KIND.values().forEach(type -> event.registerBlockEntity(
                BotaniaNeoForgeCapabilities.getBlockApiLookupById(WandBindable.LOOKUP),
                type.get(), (flower, direction) -> flower));
    }
}
