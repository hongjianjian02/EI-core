package com.mysterious.mystic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class VoidCallerItem extends Item {
    public VoidCallerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        BlockPos source = context.getClickedPos().above();
        if (!level.getEntitiesOfClass(VoidHerrscher.class, new AABB(source).inflate(32.0D)).isEmpty()) {
            return InteractionResult.FAIL;
        }

        VoidHerrscher boss = ModEntities.VOID_HERRSCHER.get().create(level);
        if (boss == null) {
            return InteractionResult.FAIL;
        }

        boss.moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D,
                player == null ? 0.0F : player.getYRot(), 0.0F);
        boss.setSource(source);
        level.addFreshEntity(boss);

        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
