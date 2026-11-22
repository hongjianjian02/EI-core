package com.mysterious.mystic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ImmortalFeastCropBlock extends CropBlock {
    public static final MapCodec<ImmortalFeastCropBlock> CODEC = simpleCodec(ImmortalFeastCropBlock::new);

    public ImmortalFeastCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ImmortalFeastCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.IMMORTAL_FEAST_SEEDS.get();
    }
}
