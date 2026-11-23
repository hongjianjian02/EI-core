package com.mysterious.mystic.scribe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record ScribeState(int unlockedMask, int selectedIndex) {
    private static final int SELECTION_SHIFT = 6;
    public static final ScribeState EMPTY = new ScribeState(0, -1);
    public static final Codec<ScribeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, ScribeMode.ALL_MASK).fieldOf("unlocked").forGetter(ScribeState::unlockedMask),
            Codec.intRange(-1, ScribeMode.MODES.size() - 1).fieldOf("selected").forGetter(ScribeState::selectedIndex)
    ).apply(instance, ScribeState::new));
    public static final StreamCodec<ByteBuf, ScribeState> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(ScribeState::unpack, ScribeState::pack);

    public ScribeState {
        unlockedMask &= ScribeMode.ALL_MASK;
        ScribeMode choice = ScribeMode.atIndex(selectedIndex);
        if (choice == null || (unlockedMask & choice.bit()) == 0) {
            selectedIndex = unlockedMask == 0 ? -1 : Integer.numberOfTrailingZeros(unlockedMask);
        }
    }

    public static ScribeState of(ItemStack stack) {
        return stack.getOrDefault(RitualScribe.STATE.get(), EMPTY);
    }

    static ScribeState unpack(int packed) {
        return new ScribeState(packed & ScribeMode.ALL_MASK, (packed >>> SELECTION_SHIFT) - 1);
    }

    int pack() {
        return unlockedMask | ((selectedIndex + 1) << SELECTION_SHIFT);
    }

    public boolean has(ScribeMode mode) {
        return (unlockedMask & mode.bit()) != 0;
    }

    public ScribeMode selected() {
        return ScribeMode.atIndex(selectedIndex);
    }

    public ScribeState unlock(ScribeMode mode) {
        return new ScribeState(unlockedMask | mode.bit(), selectedIndex);
    }

    ScribeState select(ScribeMode mode) {
        return new ScribeState(unlockedMask, mode.ordinal());
    }
}
