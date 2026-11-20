package com.mysterious.mystic.foodbox;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class FlavorBoxContents {
    public static final FlavorBoxContents EMPTY = new FlavorBoxContents(List.of());
    public static final Codec<FlavorBoxContents> CODEC = ItemStack.CODEC.listOf()
            .xmap(FlavorBoxContents::new, FlavorBoxContents::orderedItems);
    public static final StreamCodec<RegistryFriendlyByteBuf, FlavorBoxContents> STREAM_CODEC = ItemStack.STREAM_CODEC
            .apply(ByteBufCodecs.list()).map(FlavorBoxContents::new, FlavorBoxContents::orderedItems);

    // Logical order is reversed(prefix) followed by suffix. Published nodes and their stacks are never mutated.
    private final Entry prefix;
    private final Entry suffix;
    private final int size;
    private final int hash;

    private record Entry(ItemStack food, Entry next) {
        static Entry reverseOnto(Entry source, Entry end) {
            for (Entry entry = source; entry != null; entry = entry.next) {
                end = new Entry(entry.food, end);
            }
            return end;
        }
    }

    public FlavorBoxContents(List<ItemStack> items) {
        Entry head = null;
        int count = 0;
        int contentHash = 0;
        for (ItemStack stack : items.reversed()) {
            if (!stack.isEmpty()) {
                ItemStack food = stack.copy();
                head = new Entry(food, head);
                contentHash += servingHash(food);
                count++;
            }
        }
        prefix = null;
        suffix = head;
        size = count;
        hash = contentHash;
    }

    private FlavorBoxContents(Entry prefix, Entry suffix, int size, int hash) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.size = size;
        this.hash = hash;
    }

    private static int servingHash(ItemStack food) {
        return 31 * ItemStack.hashItemAndComponents(food) + food.getCount();
    }

    int size() {
        return size;
    }

    private List<ItemStack> orderedItems() {
        List<ItemStack> items = new ArrayList<>(size);
        for (Entry entry = prefix; entry != null; entry = entry.next) {
            items.add(entry.food);
        }
        Collections.reverse(items);
        for (Entry entry = suffix; entry != null; entry = entry.next) {
            items.add(entry.food);
        }
        return items;
    }

    public List<ItemStack> copyItems() {
        List<ItemStack> copy = orderedItems();
        copy.replaceAll(ItemStack::copy);
        return copy;
    }

    Set<Item> foodTypes() {
        Set<Item> types = new HashSet<>();
        orderedItems().forEach(food -> types.add(food.getItem()));
        return types;
    }

    boolean anyServing(Predicate<ItemStack> predicate) {
        return anyServing(prefix, predicate) || anyServing(suffix, predicate);
    }

    private static boolean anyServing(Entry head, Predicate<ItemStack> predicate) {
        for (Entry entry = head; entry != null; entry = entry.next) {
            if (entry.food.getCount() == 1 && predicate.test(entry.food.copy())) {
                return true;
            }
        }
        return false;
    }

    FlavorBoxContents append(ItemStack serving) {
        ItemStack owned = serving.copy();
        Entry tail = Entry.reverseOnto(suffix, prefix);
        return new FlavorBoxContents(new Entry(owned, tail), null, size + 1, hash + servingHash(owned));
    }

    Tasting beginTasting() {
        return new Tasting();
    }

    final class Tasting {
        private Entry kept;
        private Entry next = Entry.reverseOnto(prefix, suffix);
        private int remainingSize = size;
        private int remainingHash = hash;

        boolean hasNext() {
            return next != null;
        }

        ItemStack serving() {
            return next.food.copy();
        }

        void skip() {
            kept = new Entry(next.food, kept);
            next = next.next;
        }

        FlavorBoxContents take() {
            remainingSize--;
            remainingHash -= servingHash(next.food);
            next = next.next;
            return remainingSize == 0 ? EMPTY : new FlavorBoxContents(kept, next, remainingSize, remainingHash);
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof FlavorBoxContents contents && size == contents.size
                && hash == contents.hash && ItemStack.listMatches(orderedItems(), contents.orderedItems());
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
