package com.mysterious.mystic.compat.anvilguide.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;

/** Owns each browser's component samples for the current enchantment registry. */
public final class AnvilBrowserData {
    private static final Map<String, List<ItemStack>> PROVIDER_SAMPLES = new LinkedHashMap<>();
    private static Registry<Enchantment> registry;
    private static RegistryAccess access;

    private AnvilBrowserData() {
    }

    /** Replaces this provider's samples while retaining other browsers on the same registry. */
    public static synchronized void build(String provider, RegistryAccess registries, List<ItemStack> samples) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(registries, "registries");
        Objects.requireNonNull(samples, "samples");
        Registry<Enchantment> current = registries.registryOrThrow(Registries.ENCHANTMENT);
        // Holder equality can compare registration keys across worlds; the registry identity cannot.
        if (registry != current) {
            reset();
            registry = current;
        }
        access = registries;
        PROVIDER_SAMPLES.put(provider, copySamples(samples));
        rebuild();
    }

    /** Removes only this browser's samples; remaining browsers keep their own variants. */
    public static synchronized void remove(String provider) {
        Objects.requireNonNull(provider, "provider");
        if (PROVIDER_SAMPLES.remove(provider) == null) {
            return;
        }
        if (PROVIDER_SAMPLES.isEmpty()) {
            reset();
        } else {
            rebuild();
        }
    }

    /** Releases all browser sources and the world data on logout or data reload. */
    public static synchronized void reset() {
        PROVIDER_SAMPLES.clear();
        registry = null;
        access = null;
        AnvilEnchantData.reset();
    }

    private static List<ItemStack> copySamples(List<ItemStack> samples) {
        List<ItemStack> copied = new ArrayList<>(samples.size());
        for (ItemStack stack : samples) {
            if (stack != null && !stack.isEmpty()) {
                copied.add(stack.copy());
            }
        }
        return List.copyOf(copied);
    }

    private static void rebuild() {
        List<ItemStack> merged = new ArrayList<>();
        for (List<ItemStack> samples : PROVIDER_SAMPLES.values()) {
            for (ItemStack stack : samples) {
                // Neither a caller nor the derived index owns the stored provider snapshots.
                merged.add(stack.copy());
            }
        }
        AnvilEnchantData.build(access, merged);
    }
}
