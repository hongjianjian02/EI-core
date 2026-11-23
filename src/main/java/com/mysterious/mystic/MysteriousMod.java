package com.mysterious.mystic;

import com.mysterious.mystic.scribe.RitualScribe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MysteriousMod.MODID)
public class MysteriousMod {
    public static final String MODID = "eicore";

    public MysteriousMod(IEventBus modBus) {
        com.mysterious.mystic.foodbox.FlavorBoxComponents.register(modBus);
        RitualScribe.register(modBus);
        AncientSeedTrowelItem.register(modBus);
        ModBlocks.register(modBus);
        ModWorldgen.register(modBus);
        ModEntities.register(modBus);
        ModItems.register(modBus);
        ModLootModifiers.register(modBus);
        ModCreativeTabs.register(modBus);
        com.mysterious.mystic.flower.ModFlowers.register(modBus);
        com.mysterious.mystic.flower.FlowerEvents.register();
        modBus.addListener(YoukaiHomecomingMigration::registerAliases);
        NeoForge.EVENT_BUS.register(CrownHandler.class);
        NeoForge.EVENT_BUS.register(StarRingItem.class);
    }
}
