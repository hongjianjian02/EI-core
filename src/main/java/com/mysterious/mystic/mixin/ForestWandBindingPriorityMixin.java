package com.mysterious.mystic.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.common.item.WandOfTheForestItem;

@Mixin(value = WandOfTheForestItem.class, priority = 1100)
public abstract class ForestWandBindingPriorityMixin {
    @Shadow(remap = false)
    private static boolean tryCompleteBinding(Level level, Player player, ItemStack stack,
                                               GlobalPos source, BlockPos target, Direction face) {
        throw new AssertionError("Mixin shadow");
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void eicore$completeNativePoolBinding(UseOnContext context,
                                                  CallbackInfoReturnable<InteractionResult> callback) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) return;

        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        if (!(level.getBlockEntity(target) instanceof ManaPool)) return;

        ItemStack stack = context.getItemInHand();
        var source = WandOfTheForestItem.getBindingAttempt(stack);
        // ExtraBotany selects pools before Botania can finish an existing native binding.
        // With no native source, leave its pool -> Manalink selection entirely unchanged.
        if (source.isPresent() && tryCompleteBinding(level, player, stack, source.get(),
                target, context.getClickedFace())) {
            callback.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
