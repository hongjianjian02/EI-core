package com.mysterious.mystic.mixin;

import com.mysterious.mystic.flower.FlowerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CropBlock.class)
public class CropBonemealFlowerMixin {
    @Inject(method = "performBonemeal", at = @At("RETURN"))
    private void eicore$flowerGrowth(ServerLevel level, RandomSource random, BlockPos pos,
                                    BlockState state, CallbackInfo callback) {
        FlowerEvents.cropGrown(level, pos, state, level.getBlockState(pos));
    }
}
