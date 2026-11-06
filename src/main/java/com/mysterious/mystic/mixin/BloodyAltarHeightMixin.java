package com.mysterious.mystic.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 把血腥祭坛的硬编码生成高度从 33 改到 60。
 * 目标类：com.eeeab.eeeabsmobs.server.world.structure.StructureBloodyAltar
 */
@Mixin(targets = "com.eeeab.eeeabsmobs.server.world.structure.StructureBloodyAltar")
public class BloodyAltarHeightMixin {

    @ModifyConstant(method = "findGenerationPoint", constant = @Constant(intValue = 33))
    private int eicore$raiseY(int original) {
        return 60;
    }
}