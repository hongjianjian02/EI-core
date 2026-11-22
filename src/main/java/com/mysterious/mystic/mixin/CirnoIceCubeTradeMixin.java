package com.mysterious.mystic.mixin;

import com.mysterious.mystic.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.xkmc.youkaishomecoming.content.entity.characters.fairy.CirnoEntity", remap = false)
public abstract class CirnoIceCubeTradeMixin {
    @Inject(method = "getWantedItem()Lnet/minecraft/world/item/Item;", at = @At("HEAD"),
            cancellable = true, require = 1, allow = 1)
    private void eicore$useIceCubeForFrogTrades(CallbackInfoReturnable<Item> callback) {
        callback.setReturnValue(ModItems.ICE_CUBE.get());
    }

    @Redirect(
            method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/core/Holder;)Z"),
            require = 1,
            allow = 1
    )
    private boolean eicore$allowTradingWithIceCube(Player player, Holder<MobEffect> effect,
                                                  Player interactingPlayer, InteractionHand hand) {
        return player.hasEffect(effect) || interactingPlayer.getItemInHand(hand).is(ModItems.ICE_CUBE.get());
    }
}
