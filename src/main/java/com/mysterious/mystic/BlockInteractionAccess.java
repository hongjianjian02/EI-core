package com.mysterious.mystic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.fml.ModList;

public final class BlockInteractionAccess {
    private BlockInteractionAccess() {
    }

    public static boolean permitted(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        if (!player.serverLevel().mayInteract(player, pos) || !player.canInteractWithBlock(pos, 0)) {
            return false;
        }
        return !ModList.get().isLoaded("ftbchunks") || Claims.permitted(player, hand, pos);
    }

    private static final class Claims {
        static boolean permitted(ServerPlayer player, InteractionHand hand, BlockPos pos) {
            var api = dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api();
            return api != null && api.isManagerLoaded() && !api.getManager().shouldPreventInteraction(
                    player, hand, pos, dev.ftb.mods.ftbchunks.api.Protection.INTERACT_BLOCK, null);
        }
    }
}
