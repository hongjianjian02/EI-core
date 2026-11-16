package com.mysterious.mystic.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep localized fluid names out of Nuclearcraft's condenser EMI recipe IDs. */
@Pseudo
@Mixin(targets = "com.nred.nuclearcraft.compat.emi.ModEmiPlugin", remap = false)
public abstract class NuclearcraftEmiFluidIdMixin {
    @Redirect(
            method = "register(Ldev/emi/emi/api/EmiRegistry;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/fluids/FluidStack;getDescriptionId()Ljava/lang/String;"),
            require = 1,
            allow = 1,
            remap = false
    )
    private String eicore$registeredFluidPath(FluidStack stack) {
        return BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString().replace(':', '/');
    }
}
