package com.mysterious.mystic.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 关闭混沌岛外围环的龙矿生成，保留岛屿、混沌水晶与守护者。
 * 目标概率已核对 Draconic Evolution 3.1.4.633 的 genRingChunk 字节码。
 */
@Mixin(targets = "com.brandon3055.draconicevolution.world.ChaosIslandFeature", remap = false)
public abstract class ChaosIslandDraconiumMixin {

    @ModifyConstant(method = "genRingChunk", constant = @Constant(floatValue = 0.001F), require = 1, allow = 1)
    private float eicore$disableRingDraconium(float original) {
        return 0.0F;
    }
}
