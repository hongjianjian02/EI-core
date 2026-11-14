package com.mysterious.mystic.mixin;

import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 允许整合包定义低于 -273°C 的气动方块冷源。
 * 8.2.24 的序列化器先读取 heatCapacity、再读取 temperature；仅放宽后者。
 */
@Pseudo
@Mixin(targets = "me.desht.pneumaticcraft.common.recipes.other.HeatPropertiesRecipeImpl$Serializer", remap = false)
public abstract class PneumaticHeatPropertiesTemperatureMixin {

    @Redirect(
            method = "lambda$new$0",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/util/ExtraCodecs;NON_NEGATIVE_INT:Lcom/mojang/serialization/Codec;",
                    ordinal = 1),
            require = 1,
            allow = 1,
            remap = false
    )
    private static Codec<Integer> eicore$allowNegativeBlockTemperatures() {
        return Codec.INT;
    }
}
