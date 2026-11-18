package com.mysterious.mystic.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum ManaDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        ManaDisplay display = ManaDisplay.of(accessor.getBlockEntity());
        if (display != null) {
            data.putInt(BotaniaJadePlugin.MANA_TAG, display.mana());
            data.putInt(BotaniaJadePlugin.MAX_MANA_TAG, display.maximum());
            data.putInt(BotaniaJadePlugin.COLOR_TAG, display.color());
        }
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor) {
        return ManaDisplay.isSupported(accessor.getBlockEntity());
    }

    @Override
    public ResourceLocation getUid() {
        return BotaniaJadePlugin.MANA;
    }
}
