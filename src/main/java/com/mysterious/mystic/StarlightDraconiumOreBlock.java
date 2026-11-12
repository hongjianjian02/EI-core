package com.mysterious.mystic;

import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class StarlightDraconiumOreBlock extends DropExperienceBlock {
    private static final UniformInt EXPERIENCE = UniformInt.of(5, 12);

    public StarlightDraconiumOreBlock(Properties properties) {
        super(ConstantInt.of(0), properties);
    }

    @Override
    public int getExpDrop(BlockState state, LevelAccessor level, BlockPos pos,
                          BlockEntity blockEntity, Entity breaker, ItemStack tool) {
        return EXPERIENCE.sample(level.getRandom());
    }
}
