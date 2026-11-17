package com.mysterious.mystic;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.ModifyRegistriesEvent;
import net.neoforged.neoforge.registries.callback.BakeCallback;
import org.slf4j.Logger;

public final class YoukaiHomecomingMigration {
    private static final Logger LOGGER = LogUtils.getLogger();

    private YoukaiHomecomingMigration() {
    }

    public static void registerAliases(ModifyRegistriesEvent event) {
        for (Registry<?> registry : event.getRegistries()) {
            attachAliases(registry);
        }
    }

    private static <T> void attachAliases(Registry<T> registry) {
        registry.addCallback((BakeCallback<T>) YoukaiHomecomingMigration::addAliases);
    }

    private static <T> void addAliases(Registry<T> registry) {
        int aliases = 0;
        for (ResourceLocation target : registry.keySet()) {
            if (!target.getNamespace().equals("youkaishomecoming")) {
                continue;
            }
            ResourceLocation legacy = ResourceLocation.fromNamespaceAndPath("youkaisfeasts", target.getPath());
            if (!registry.containsKey(legacy)) {
                registry.addAlias(legacy, target);
                aliases++;
            }
        }
        if (registry.key().equals(Registries.ITEM)) {
            ResourceLocation seeds = ResourceLocation.fromNamespaceAndPath("youkaishomecoming", "udumbara_seeds");
            if (registry.containsKey(seeds)) {
                registry.addAlias(ResourceLocation.fromNamespaceAndPath("youkaisfeasts", "udumbara_leaves"), seeds);
                aliases++;
            }
        }
        if (aliases > 0) {
            LOGGER.info("Registered {} Youkais' Feasts aliases in {}", aliases, registry.key().location());
        }
    }
}
