package com.mysterious.mystic.flower;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import vazkii.botania.common.item.block.SpecialFlowerBlockItem;

public final class GeneratingFlowerItem extends SpecialFlowerBlockItem {
    private final FlowerKind kind;

    public GeneratingFlowerItem(Block block, Properties properties, FlowerKind kind) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.eicore." + kind.id).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.eicore.generating_flower").withStyle(ChatFormatting.DARK_AQUA));
    }
}
