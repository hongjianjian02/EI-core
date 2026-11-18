package com.mysterious.mystic.foodbox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

final class FlavorBoxContainers {
    record Source(IItemHandler handler, List<BlockEntity> blocks, IntPredicate isInfinite, boolean stableHandler) {
    }

    private FlavorBoxContainers() {
    }

    static boolean permitted(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        if (!player.serverLevel().mayInteract(player, pos) || !player.canInteractWithBlock(pos, 0)) {
            return false;
        }
        return !ModList.get().isLoaded("ftbchunks") || Claims.permitted(player, hand, pos);
    }

    static Source find(ServerPlayer player, InteractionHand hand, BlockPos pos, Direction side) {
        BlockEntity block = player.serverLevel().getBlockEntity(pos);
        if (block == null || block.isRemoved() || !permitted(player, hand, pos)) {
            return null;
        }
        if (ModList.get().isLoaded("sophisticatedstorage") && Sophisticated.isStorage(block)) {
            return Sophisticated.storage(player, hand, pos, side, block);
        }
        if (ModList.get().isLoaded("sophisticatedbackpacks") && Sophisticated.isBackpack(block)) {
            return Sophisticated.backpack(player, pos, side, block);
        }
        BlockState state = player.serverLevel().getBlockState(pos);
        if (!(block instanceof BaseContainerBlockEntity container)
                || !(state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock
                || state.getBlock() instanceof ShulkerBoxBlock)
                || !container.stillValid(player) || !container.canOpen(player)) {
            return null;
        }
        if (container instanceof ShulkerBoxBlockEntity shulker && shulker.isClosed()
                && !player.serverLevel().noCollision(Shulker.getProgressDeltaAabb(1.0F,
                state.getValue(ShulkerBoxBlock.FACING), 0.0F, 0.5F).move(pos).deflate(1.0E-6))) {
            return null;
        }
        List<BlockEntity> blocks = new ArrayList<>();
        blocks.add(block);
        if (state.getBlock() instanceof ChestBlock chest) {
            if (ChestBlock.getContainer(chest, state, player.serverLevel(), pos, false) == null) {
                return null;
            }
            if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
                if (!permitted(player, hand, otherPos)
                        || !(player.serverLevel().getBlockEntity(otherPos) instanceof BaseContainerBlockEntity other)
                        || !other.canOpen(player)) {
                    return null;
                }
                blocks.add(other);
            }
        }
        IItemHandler handler = player.serverLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        return handler == null ? null : new Source(handler, List.copyOf(blocks), slot -> false, false);
    }

    private static final class Claims {
        static boolean permitted(ServerPlayer player, InteractionHand hand, BlockPos pos) {
            var api = dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api();
            return api != null && api.isManagerLoaded() && !api.getManager().shouldPreventInteraction(
                    player, hand, pos, dev.ftb.mods.ftbchunks.api.Protection.INTERACT_BLOCK, null);
        }
    }

    private static final class Sophisticated {
        static boolean isStorage(BlockEntity block) {
            return block instanceof net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
        }

        static boolean isBackpack(BlockEntity block) {
            return block instanceof net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
        }

        static Source storage(ServerPlayer player, InteractionHand hand, BlockPos pos, Direction side, BlockEntity block) {
            var storage = (net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity) block;
            List<BlockEntity> blocks = new ArrayList<>();
            blocks.add(block);
            BlockState state = player.serverLevel().getBlockState(pos);
            if (storage instanceof net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity shulker
                    && shulker.isClosed() && !player.serverLevel().noCollision(Shulker.getProgressDeltaAabb(1.0F,
                    state.getValue(net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock.FACING),
                    0.0F, 0.5F).move(pos).deflate(1.0E-6))) {
                return null;
            }
            if (storage instanceof net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity chest) {
                if (net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock.isChestBlockedAt(player.serverLevel(), pos)) {
                    return null;
                }
                if (state.getValue(net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock.TYPE) != ChestType.SINGLE) {
                    BlockPos otherPos = pos.relative(net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock.getConnectedDirection(state));
                    if (!permitted(player, hand, otherPos)
                            || net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock.isChestBlockedAt(player.serverLevel(), otherPos)
                            || !(player.serverLevel().getBlockEntity(otherPos) instanceof net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity other)) {
                        return null;
                    }
                    blocks.add(other);
                }
                storage = chest.getMainChestBlockEntity();
                if (storage == null) {
                    return null;
                }
            }
            for (BlockEntity part : blocks) {
                var partStorage = (net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity) part;
                if (partStorage.isLinkedStorage() || partStorage.isBeingUpgraded()
                        || partStorage instanceof net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity wood && wood.isPacked()) {
                    return null;
                }
            }
            IItemHandler handler = player.serverLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            var inventory = storage.getStorageWrapper().getInventoryHandler();
            return handler == null ? null : new Source(handler, List.copyOf(blocks),
                    slot -> inventory.getInventoryPartitioner().isInfinite(slot), true);
        }

        static Source backpack(ServerPlayer player, BlockPos pos, Direction side, BlockEntity block) {
            var backpack = (net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity) block;
            if (backpack.getLinkedStorageEndpointData() != null) {
                return null;
            }
            IItemHandler handler = player.serverLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            var inventory = backpack.getStorageWrapper().getInventoryHandler();
            return handler == null ? null : new Source(handler, List.of(block),
                    slot -> inventory.getInventoryPartitioner().isInfinite(slot), true);
        }
    }
}
