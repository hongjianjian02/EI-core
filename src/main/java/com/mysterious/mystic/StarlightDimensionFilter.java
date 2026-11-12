package com.mysterious.mystic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public final class StarlightDimensionFilter extends PlacementFilter {
    private static final StarlightDimensionFilter INSTANCE = new StarlightDimensionFilter();
    public static final MapCodec<StarlightDimensionFilter> CODEC = MapCodec.unit(INSTANCE);
    private static final ResourceKey<Level> STARLIGHT = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("eternal_starlight", "starlight"));

    private StarlightDimensionFilter() {
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return context.getLevel().getLevel().dimension().equals(STARLIGHT);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModWorldgen.STARLIGHT_DIMENSION.get();
    }
}
