package com.mysterious.mystic;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MysteriousMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.eicore.main"))
                    .icon(() -> new ItemStack(ModItems.UNSTABLE_RUNE_GEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.UNSTABLE_RUNE_GEM.get());
                        output.accept(ModItems.ACTIVATED_RUNE_GEM.get());
                        output.accept(ModItems.TIANSHU_STAR.get());
                        output.accept(ModItems.STAR_RING.get());
                        output.accept(ModItems.MITHRIL_INGOT.get());
                        output.accept(ModItems.KENGJIN_INGOT.get());
                        output.accept(ModItems.TIANGANG_INGOT.get());
                        output.accept(ModItems.TIANQING_INGOT.get());
                        output.accept(ModItems.SUNLIGHT_ALLOY_INGOT.get());
                        output.accept(ModItems.ICE_CUBE.get());
                        output.accept(ModItems.COMPRESSED_MARBLE_ITEM.get());
                        output.accept(ModItems.TIANQING_ORE_ITEM.get());
                        output.accept(ModItems.HAZE_ICE_DRACONIUM_ORE_ITEM.get());
                        output.accept(ModItems.VOIDSTONE_DRACONIUM_ORE_ITEM.get());
                        output.accept(ModItems.MITHRIL_BLOCK_ITEM.get());
                        output.accept(ModItems.KENGJIN_BLOCK_ITEM.get());
                        output.accept(ModItems.MYSTIC_CROWN.get());
                        output.accept(ModItems.ELECTRIC_EEL_SPAWN_EGG.get());
                        output.accept(ModItems.ELECTRIC_EEL_BUCKET.get());
                        output.accept(ModItems.BAIWEI_SEEDS.get());
                        output.accept(ModItems.YOUKAI_FEAST_SEEDS.get());
                        output.accept(ModItems.IMMORTAL_FEAST_SEEDS.get());
                        output.accept(ModItems.FLAVOR_BOX.get());
                        com.mysterious.mystic.flower.ModFlowers.ITEMS_BY_KIND.values()
                                .forEach(item -> output.accept(item.get()));
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
        bus.addListener(ModCreativeTabs::addVanillaTabItems);
    }

    private static void addVanillaTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.insertAfter(Items.PUFFERFISH_BUCKET.getDefaultInstance(), ModItems.ELECTRIC_EEL_BUCKET.toStack(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
