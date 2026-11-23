package com.mysterious.mystic.scribe;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public enum ScribeMode {
    AIR("air"),
    FIRE("fire"),
    WATER("water"),
    EARTH("earth"),
    TENEBRAE("tenebrae"),
    DEUS("deus");

    public static final List<ScribeMode> MODES = List.of(values());
    static final int ALL_MASK = (1 << MODES.size()) - 1;
    private final String key;
    private final ResourceLocation toolId;

    ScribeMode(String key) {
        this.key = key;
        this.toolId = ResourceLocation.fromNamespaceAndPath("neovitae", key + "_scribe_tool");
    }

    public String key() {
        return key;
    }

    public ResourceLocation toolId() {
        return toolId;
    }

    int bit() {
        return 1 << ordinal();
    }

    public Component label() {
        return Component.translatable("item.eicore.ritual_scribe_tool.mode." + key);
    }

    public ItemStack tool() {
        return BuiltInRegistries.ITEM.getOptional(toolId).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    static ScribeMode forTool(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (ScribeMode mode : MODES) {
            if (mode.toolId.equals(id)) return mode;
        }
        return null;
    }

    static ScribeMode atIndex(int index) {
        return index >= 0 && index < MODES.size() ? MODES.get(index) : null;
    }
}
