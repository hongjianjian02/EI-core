package com.mysterious.mystic.compat.lootr;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import noobanidus.mods.lootr.common.api.ILootrBlockEntityConverter;
import noobanidus.mods.lootr.common.api.data.blockentity.ILootrBlockEntity;
import org.jetbrains.annotations.Nullable;

/** Lootr discovers this adapter through its official service loader. */
public final class SpiritCacheLootrConverter implements ILootrBlockEntityConverter<BlockEntity> {
    @Override
    public BlockEntityType<?> getBlockEntityType() {
        // Registry lookup keeps this service loadable when Neo Vitae is absent.
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("neovitae", "spirit_cache"));
    }

    @Override
    @Nullable
    public ILootrBlockEntity apply(BlockEntity blockEntity) {
        return blockEntity instanceof SpiritCacheLootrAccess access ? access.eicore$getLootrProvider() : null;
    }
}
