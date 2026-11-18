package com.mysterious.mystic.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ProgressStyle;

public enum ManaComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!ManaDisplay.isSupported(accessor.getBlockEntity())) {
            return;
        }
        CompoundTag data = accessor.getServerData();
        if (!data.contains(BotaniaJadePlugin.MANA_TAG, Tag.TAG_INT)
                || !data.contains(BotaniaJadePlugin.MAX_MANA_TAG, Tag.TAG_INT)) {
            return;
        }
        int maximum = data.getInt(BotaniaJadePlugin.MAX_MANA_TAG);
        if (maximum <= 0) {
            return;
        }
        int mana = Math.max(0, Math.min(maximum, data.getInt(BotaniaJadePlugin.MANA_TAG)));
        IElementHelper helper = IElementHelper.get();
        ProgressStyle style = helper.progressStyle()
                .color(0xFF000000 | data.getInt(BotaniaJadePlugin.COLOR_TAG)).textColor(0xFFFFFFFF);
        tooltip.add(helper.progress((float) mana / maximum,
                Component.translatable("jade.eicore.mana", mana, maximum),
                style, BoxStyle.getNestedBox(), true));
    }

    @Override
    public ResourceLocation getUid() {
        return BotaniaJadePlugin.MANA;
    }

    @Override
    public int getDefaultPriority() {
        return 9000;
    }
}
