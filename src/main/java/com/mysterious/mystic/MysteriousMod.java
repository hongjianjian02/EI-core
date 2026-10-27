package com.mysterious.mystic;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MysteriousMod.MODID)
public class MysteriousMod {
    public static final String MODID = "eicore";

    public MysteriousMod(IEventBus modBus) {
        ModItems.register(modBus);
        NeoForge.EVENT_BUS.register(CrownHandler.class);
    }
}