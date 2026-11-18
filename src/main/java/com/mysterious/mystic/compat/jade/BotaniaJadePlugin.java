package com.mysterious.mystic.compat.jade;

import com.mysterious.mystic.MysteriousMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(MysteriousMod.MODID)
public final class BotaniaJadePlugin implements IWailaPlugin {
    static final ResourceLocation MANA = ResourceLocation.fromNamespaceAndPath(
            MysteriousMod.MODID, "botania_mana");
    static final String MANA_TAG = "eicore_mana";
    static final String MAX_MANA_TAG = "eicore_max_mana";
    static final String COLOR_TAG = "eicore_mana_color";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ManaDataProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ManaComponentProvider.INSTANCE, Block.class);
    }
}
