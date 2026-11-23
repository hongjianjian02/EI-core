package com.mysterious.mystic.mixin;

import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.xkmc.l2tabs.compat.api.AccessoriesMultiplex", remap = false)
public abstract class L2TabsCuriosBackendMixin {
    @Redirect(
            method = "get()Ldev/xkmc/l2tabs/compat/api/AccessoriesMultiplex;",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/ModList;isLoaded(Ljava/lang/String;)Z", ordinal = 1),
            require = 1,
            allow = 1
    )
    private static boolean eicore$useCuriosForBridgedSlots(ModList mods, String modId) {
        // The bridge stores equipment in Curios, but its Accessories view exposes only mapped slots.
        return !("accessories".equals(modId) && mods.isLoaded("accessories_to_curios")) && mods.isLoaded(modId);
    }
}
