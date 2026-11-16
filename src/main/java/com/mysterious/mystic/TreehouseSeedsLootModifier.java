package com.mysterious.mystic;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class TreehouseSeedsLootModifier extends LootModifier {
    public static final MapCodec<TreehouseSeedsLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, TreehouseSeedsLootModifier::new));

    public TreehouseSeedsLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        var random = context.getRandom();
        var seed = random.nextBoolean() ? ModItems.BAIWEI_SEEDS.get() : ModItems.YOUKAI_FEAST_SEEDS.get();
        generatedLoot.add(new ItemStack(seed, 1 + random.nextInt(2)));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
