package com.mysterious.mystic.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 把核心熔炉遗迹的硬编码生成高度从 -20 改到 120。
 * 目标类：com.eeeab.eeeabsmobs.server.world.structure.StructureCoreforgeRuins
 */
@Mixin(targets = "com.eeeab.eeeabsmobs.server.world.structure.StructureCoreforgeRuins")
public class CoreforgeRuinsHeightMixin {

    @ModifyConstant(method = "findGenerationPoint", constant = @Constant(intValue = -20))
    private int eicore$raiseY(int original) {
        return 120;
    }
}