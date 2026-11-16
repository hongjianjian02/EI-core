package com.mysterious.mystic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class YoukaiFeastCropBlock extends CropBlock {
    public static final MapCodec<YoukaiFeastCropBlock> CODEC = simpleCodec(YoukaiFeastCropBlock::new);

    public YoukaiFeastCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<YoukaiFeastCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.YOUKAI_FEAST_SEEDS.get();
    }
}
