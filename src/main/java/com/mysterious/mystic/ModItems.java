package com.mysterious.mystic;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MysteriousMod.MODID);

    public static final DeferredItem<Item> MYSTIC_CROWN =
            ITEMS.registerItem("mystic_crown", Item::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> UNSTABLE_RUNE_GEM =
            ITEMS.registerItem("unstable_rune_gem", Item::new, new Item.Properties());

    public static final DeferredItem<Item> ACTIVATED_RUNE_GEM =
            ITEMS.registerItem("activated_rune_gem", GlintItem::new, new Item.Properties());

    public static final DeferredItem<Item> TIANSHU_STAR =
            ITEMS.registerItem("tianshu_star", Item::new, new Item.Properties());

    public static final DeferredItem<Item> MITHRIL_INGOT =
            ITEMS.registerItem("mithril_ingot", Item::new, new Item.Properties());

    public static final DeferredItem<Item> KENGJIN_INGOT =
            ITEMS.registerItem("kengjin_ingot", Item::new, new Item.Properties());

    public static final DeferredItem<Item> TIANGANG_INGOT =
            ITEMS.registerItem("tiangang_ingot", Item::new, new Item.Properties());

    public static final DeferredItem<Item> TIANQING_INGOT =
            ITEMS.registerItem("tianqing_ingot", Item::new, new Item.Properties());

    public static final DeferredItem<Item> VOIDCALLER =
            ITEMS.registerItem("voidcaller", VoidCallerItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<BlockItem> COMPRESSED_MARBLE_ITEM =
            ITEMS.registerSimpleBlockItem("compressed_marble", ModBlocks.COMPRESSED_MARBLE);

    public static final DeferredItem<BlockItem> TIANQING_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("tianqing_ore", ModBlocks.TIANQING_ORE);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}