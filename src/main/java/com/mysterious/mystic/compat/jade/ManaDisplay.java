package com.mysterious.mystic.compat.jade;

import net.minecraft.world.level.block.entity.BlockEntity;
import vazkii.botania.api.block_entity.BindableSpecialFlowerBlockEntity;
import vazkii.botania.api.mana.ManaCollector;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.common.block.block_entity.BotanicalBreweryBlockEntity;
import vazkii.botania.common.block.block_entity.ManaEnchanterBlockEntity;
import vazkii.botania.common.block.block_entity.RunicAltarBlockEntity;

record ManaDisplay(int mana, int maximum, int color) {
    private static final int DEFAULT_COLOR = 0x55CACA;

    static boolean isSupported(BlockEntity entity) {
        return entity instanceof BindableSpecialFlowerBlockEntity<?>
                || entity instanceof ManaPool || entity instanceof ManaCollector
                || entity instanceof RunicAltarBlockEntity || entity instanceof BotanicalBreweryBlockEntity
                || entity instanceof ManaEnchanterBlockEntity;
    }

    static ManaDisplay of(BlockEntity entity) {
        if (entity instanceof BindableSpecialFlowerBlockEntity<?> flower) {
            return create(flower.getMana(), flower.getMaxMana(), flower.getColor());
        }
        if (entity instanceof ManaPool pool) {
            return create(pool.getCurrentMana(), pool.getMaxMana(), DEFAULT_COLOR);
        }
        if (entity instanceof ManaCollector collector) {
            return create(collector.getCurrentMana(), collector.getMaxMana(), DEFAULT_COLOR);
        }
        if (entity instanceof RunicAltarBlockEntity altar) {
            return create(altar.getCurrentMana(), altar.getTargetMana(), DEFAULT_COLOR);
        }
        if (entity instanceof BotanicalBreweryBlockEntity brewery) {
            return create(brewery.getCurrentMana(), brewery.getManaCost(), DEFAULT_COLOR);
        }
        if (entity instanceof ManaEnchanterBlockEntity enchanter) {
            int mana = enchanter.getCurrentMana();
            return create(mana, mana + enchanter.getAvailableSpaceForMana(), DEFAULT_COLOR);
        }
        return null;
    }

    private static ManaDisplay create(int mana, int maximum, int color) {
        return maximum > 0 ? new ManaDisplay(Math.max(0, Math.min(maximum, mana)), maximum, color) : null;
    }
}
