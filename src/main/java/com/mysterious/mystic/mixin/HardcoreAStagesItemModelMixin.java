package com.mysterious.mystic.mixin;

import java.util.List;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.liquor.hardcoreastagesitem.utils.ModelOperation", remap = false)
public abstract class HardcoreAStagesItemModelMixin {
    @Inject(method = "replaceModel(Ljava/util/List;Z)V", at = @At("HEAD"), cancellable = true)
    private static void eicore$keepOriginalItemModels(List<Item> items, boolean lock, CallbackInfo callback) {
        callback.cancel();
    }
}
