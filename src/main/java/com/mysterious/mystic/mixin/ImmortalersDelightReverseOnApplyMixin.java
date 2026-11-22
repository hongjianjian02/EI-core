package com.mysterious.mystic.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Use a registry holder for effects replaced by Magical Reverse when applied. */
@Pseudo
@Mixin(targets = "com.renyigesai.immortalers_delight.potion.MagicalReversePotionEffect", remap = false)
public abstract class ImmortalersDelightReverseOnApplyMixin {
    @Redirect(
            method = "onEntityAddEffect(Lnet/neoforged/neoforge/event/entity/living/MobEffectEvent$Applicable;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/Holder;direct(Ljava/lang/Object;)Lnet/minecraft/core/Holder;"),
            require = 1,
            allow = 1,
            remap = false
    )
    private static Holder<MobEffect> eicore$registeredEffectHolder(Object effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder((MobEffect) effect);
    }
}
