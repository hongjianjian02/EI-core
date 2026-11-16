package com.mysterious.mystic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BaiweiCropBlock extends CropBlock {
    public static final MapCodec<BaiweiCropBlock> CODEC = simpleCodec(BaiweiCropBlock::new);

    public BaiweiCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BaiweiCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.BAIWEI_SEEDS.get();
    }
}
