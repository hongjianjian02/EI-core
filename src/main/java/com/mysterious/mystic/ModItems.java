package com.mysterious.mystic;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MysteriousMod.MODID);

    public static final DeferredItem<Item> MYSTIC_CROWN =
            ITEMS.registerItem("mystic_crown", Item::new, new Item.Properties().stacksTo(1));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}