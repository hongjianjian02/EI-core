package com.mysterious.mystic.mixin;

import com.breakinblocks.neovitae.common.block.SpiritCacheBlock;
import com.mysterious.mystic.compat.lootr.SpiritCacheLootrAccess;
import com.mysterious.mystic.compat.lootr.SpiritCacheLootrProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpiritCacheBlock.class, remap = false)
public abstract class SpiritCacheLootrBlockMixin extends Block {
    protected SpiritCacheLootrBlockMixin(Properties properties) { super(properties); }

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void eicore$openPersonalLoot(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                                        CallbackInfoReturnable<InteractionResult> callback) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof SpiritCacheLootrAccess access) {
            SpiritCacheLootrProvider provider = access.eicore$getLootrProvider();
            if (provider != null) {
                provider.open(serverPlayer);
                callback.setReturnValue(InteractionResult.CONSUME);
            }
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SpiritCacheLootrAccess access) {
            access.eicore$markPlayerPlaced();
        }
    }
}
