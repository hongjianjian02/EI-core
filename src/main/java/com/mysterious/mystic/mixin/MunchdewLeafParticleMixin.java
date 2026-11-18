package com.mysterious.mystic.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Consumable leaf blocks need not have a corresponding item for particles. */
@Mixin(targets = "vazkii.botania.common.block.block_entity.flower.generating.MunchdewBlockEntity", remap = false)
public abstract class MunchdewLeafParticleMixin {
    @ModifyArg(
            method = "eatLeaves()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/particles/ItemParticleOption;<init>(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/world/item/ItemStack;)V",
                    remap = false
            ),
            index = 1,
            require = 1,
            allow = 1,
            remap = false
    )
    private ItemStack eicore$nonEmptyLeafParticle(ItemStack stack) {
        // The fallback is cosmetic; Botania still consumes the original leaf block.
        return stack.isEmpty() ? new ItemStack(Items.OAK_LEAVES) : stack;
    }
}
