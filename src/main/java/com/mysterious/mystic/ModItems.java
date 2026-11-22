package com.mysterious.mystic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MysteriousMod.MODID);

    public static final DeferredItem<com.mysterious.mystic.foodbox.FlavorBoxItem> FLAVOR_BOX =
            ITEMS.registerItem("flavor_box", com.mysterious.mystic.foodbox.FlavorBoxItem::new,
                    new Item.Properties().stacksTo(1).component(
                            net.minecraft.core.component.DataComponents.CONTAINER,
                            net.minecraft.world.item.component.ItemContainerContents.EMPTY));

    public static final DeferredItem<ItemNameBlockItem> BAIWEI_SEEDS =
            ITEMS.registerItem("baiwei_seeds", properties ->
                    new ItemNameBlockItem(ModBlocks.BAIWEI_CROP.get(), properties));

    public static final DeferredItem<ItemNameBlockItem> YOUKAI_FEAST_SEEDS =
            ITEMS.registerItem("youkai_feast_seeds", properties ->
                    new ItemNameBlockItem(ModBlocks.YOUKAI_FEAST_CROP.get(), properties));

    public static final DeferredItem<ItemNameBlockItem> IMMORTAL_FEAST_SEEDS =
            ITEMS.registerItem("immortal_feast_seeds", properties ->
                    new ItemNameBlockItem(ModBlocks.IMMORTAL_FEAST_CROP.get(), properties));

    public static final DeferredItem<Item> MYSTIC_CROWN =
            ITEMS.registerItem("mystic_crown", Item::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> UNSTABLE_RUNE_GEM =
            ITEMS.registerItem("unstable_rune_gem", Item::new, new Item.Properties());

    public static final DeferredItem<Item> ACTIVATED_RUNE_GEM =
            ITEMS.registerItem("activated_rune_gem", GlintItem::new, new Item.Properties());

    public static final DeferredItem<Item> TIANSHU_STAR =
            ITEMS.registerItem("tianshu_star", Item::new, new Item.Properties());

    public static final DeferredItem<Item> STAR_RING =
            ITEMS.registerItem("star_ring", Item::new, new Item.Properties());

    public static final DeferredItem<Item> MITHRIL_INGOT =
            ITEMS.registerItem("mithril_ingot", Item::new, new Item.Properties());

    public static final DeferredItem<Item> KENGJIN_INGOT =
            ITEMS.registerItem("kengjin_ingot", Item::new, new Item.Properties());

    public static final DeferredItem<Item> TIANGANG_INGOT =
            ITEMS.registerItem("tiangang_ingot", Item::new, new Item.Properties());

    public static final DeferredItem<Item> TIANQING_INGOT =
            ITEMS.registerItem("tianqing_ingot", Item::new, new Item.Properties());

    public static final DeferredItem<Item> SUNLIGHT_ALLOY_INGOT =
            ITEMS.registerItem("sunlight_alloy_ingot", Item::new, new Item.Properties());

    public static final DeferredItem<Item> ICE_CUBE =
            ITEMS.registerItem("ice_cube", Item::new, new Item.Properties().component(DataComponents.ITEM_NAME,
                    Component.translatable("item.eicore.ice_cube.named", Component.literal("baka").withColor(0xADD8E6))));

    public static final DeferredItem<DeferredSpawnEggItem> ELECTRIC_EEL_SPAWN_EGG =
            ITEMS.registerItem("electric_eel_spawn_egg", properties ->
                    new DeferredSpawnEggItem(ModEntities.ELECTRIC_EEL, 0x303C25, 0xB6AC66, properties));

    public static final DeferredItem<MobBucketItem> ELECTRIC_EEL_BUCKET =
            ITEMS.registerItem("electric_eel_bucket", properties ->
                    new MobBucketItem(ModEntities.ELECTRIC_EEL.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties),
                    new Item.Properties().stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY));


    public static final DeferredItem<BlockItem> COMPRESSED_MARBLE_ITEM =
            ITEMS.registerSimpleBlockItem("compressed_marble", ModBlocks.COMPRESSED_MARBLE);

    public static final DeferredItem<BlockItem> TIANQING_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("tianqing_ore", ModBlocks.TIANQING_ORE);

    public static final DeferredItem<BlockItem> HAZE_ICE_DRACONIUM_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("haze_ice_draconium_ore", ModBlocks.HAZE_ICE_DRACONIUM_ORE);

    public static final DeferredItem<BlockItem> VOIDSTONE_DRACONIUM_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("voidstone_draconium_ore", ModBlocks.VOIDSTONE_DRACONIUM_ORE);

    public static final DeferredItem<BlockItem> MITHRIL_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("mithril_block", ModBlocks.MITHRIL_BLOCK);

    public static final DeferredItem<BlockItem> KENGJIN_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("kengjin_block", ModBlocks.KENGJIN_BLOCK);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
