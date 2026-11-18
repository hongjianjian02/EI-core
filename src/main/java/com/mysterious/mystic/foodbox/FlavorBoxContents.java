package com.mysterious.mystic.foodbox;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public final class FlavorBoxContents {
    public static final FlavorBoxContents EMPTY = new FlavorBoxContents(List.of());
    public static final Codec<FlavorBoxContents> CODEC = ItemStack.CODEC.listOf()
            .xmap(FlavorBoxContents::new, contents -> contents.items);
    public static final StreamCodec<RegistryFriendlyByteBuf, FlavorBoxContents> STREAM_CODEC = ItemStack.STREAM_CODEC
            .apply(ByteBufCodecs.list()).map(FlavorBoxContents::new, contents -> contents.items);
    private final List<ItemStack> items;
    private final int hashCode;

    public FlavorBoxContents(List<ItemStack> items) {
        this.items = items.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
        this.hashCode = ItemStack.hashStackList(this.items);
    }

    public List<ItemStack> copyItems() {
        return items.stream().map(ItemStack::copy).collect(Collectors.toCollection(java.util.ArrayList::new));
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof FlavorBoxContents contents && ItemStack.listMatches(items, contents.items);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
