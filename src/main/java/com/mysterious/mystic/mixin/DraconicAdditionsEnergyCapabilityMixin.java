package com.mysterious.mystic.mixin;

import com.brandon3055.draconicevolution.api.DataComponentAccessor;
import com.brandon3055.draconicevolution.api.modules.lib.ModularOPStorage;
import com.brandon3055.draconicevolution.items.equipment.IModularEnergyItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A modular item may intentionally provide no energy storage capability. */
@Pseudo
@Mixin(targets = "net.foxmcloud.draconicadditions.lib.DAContent", remap = false)
public abstract class DraconicAdditionsEnergyCapabilityMixin {
    @Inject(
            method = "getEnergyCap(Lnet/minecraft/world/item/ItemStack;)Lcom/brandon3055/draconicevolution/api/modules/lib/ModularOPStorage;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            allow = 1,
            remap = false
    )
    private static void eicore$nullableEnergyStorage(ItemStack stack,
            CallbackInfoReturnable<ModularOPStorage> cir) {
        // Preserve the original error path for callers supplying an unrelated item.
        if (!(stack.getItem() instanceof IModularEnergyItem item)) return;
        ModularOPStorage storage = item.createOPCapForRegistration(stack);
        if (storage != null) storage.updateDataAccess(DataComponentAccessor.itemStack(stack));
        cir.setReturnValue(storage);
    }
}
