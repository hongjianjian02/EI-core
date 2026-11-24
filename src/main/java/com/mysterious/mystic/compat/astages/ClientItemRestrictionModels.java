package com.mysterious.mystic.compat.astages;

import com.alessandro.astages.api.holder.AClientHolder;
import com.alessandro.astages.engine.AClientRestrictionManager;
import com.mojang.logging.LogUtils;
import com.mysterious.mystic.MysteriousMod;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = MysteriousMod.MODID, value = Dist.CLIENT)
public final class ClientItemRestrictionModels {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int REFRESH_INTERVAL = 10;
    private static final ModelResourceLocation UNKNOWN_MODEL = inventoryModel(
            ResourceLocation.fromNamespaceAndPath("hardcoreastagesitem", "unknown_item"));
    private static final Field BAKED_REGISTRY = findBakedRegistry();
    private static final Map<ModelResourceLocation, BakedModel> ORIGINAL_MODELS = new HashMap<>();
    private static Map<ModelResourceLocation, BakedModel> activeModels;
    private static int ticksUntilRefresh;

    private ClientItemRestrictionModels() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || --ticksUntilRefresh > 0) return;
        ticksUntilRefresh = REFRESH_INTERVAL;
        refresh(minecraft);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        apply(Minecraft.getInstance(), Set.of());
        ticksUntilRefresh = 0;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("reloadmodel").executes(context -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || !refresh(minecraft)) return 0;
            minecraft.player.sendSystemMessage(Component.literal("Done!"));
            return 1;
        }));
    }

    private static boolean refresh(Minecraft minecraft) {
        // Wait for AStages to finish syncing item restrictions, including after /reload.
        if (!AClientRestrictionManager.areRestrictionsAvailable()) return false;
        var holder = AClientHolder.serverAndPlayer();
        Set<ModelResourceLocation> lockedModels = new HashSet<>();
        for (var restriction : AClientRestrictionManager.ITEM_INSTANCE.getRegistry().getItemRestrictions()) {
            for (var item : restriction.getItems()) {
                if (AClientRestrictionManager.ITEM_INSTANCE.getRestriction(holder, new ItemStack(item)) != null) {
                    lockedModels.add(inventoryModel(BuiltInRegistries.ITEM.getKey(item)));
                }
            }
        }
        return apply(minecraft, lockedModels);
    }

    private static boolean apply(Minecraft minecraft, Set<ModelResourceLocation> lockedModels) {
        if (BAKED_REGISTRY == null) return false;
        var models = readBakedRegistry(minecraft.getModelManager());
        if (models != activeModels) {
            // A resource reload publishes a fresh map with freshly baked original models.
            ORIGINAL_MODELS.clear();
            activeModels = models;
        }
        if (models == null) return false;
        BakedModel unknown = models.get(UNKNOWN_MODEL);
        if (!lockedModels.isEmpty() && unknown == null) return false;
        if (lockedModels.equals(ORIGINAL_MODELS.keySet())
                && lockedModels.stream().allMatch(location -> models.get(location) == unknown)) return true;

        boolean changed = false;
        var originals = ORIGINAL_MODELS.entrySet().iterator();
        while (originals.hasNext()) {
            var entry = originals.next();
            if (lockedModels.contains(entry.getKey())) {
                if (models.get(entry.getKey()) != unknown) {
                    models.put(entry.getKey(), unknown);
                    changed = true;
                }
                continue;
            }
            // Another renderer may have updated this entry since it was masked.
            changed |= models.replace(entry.getKey(), unknown, entry.getValue());
            originals.remove();
        }
        for (var location : lockedModels) {
            if (ORIGINAL_MODELS.containsKey(location)) continue;
            BakedModel original = models.get(location);
            if (original == null || original == unknown) continue;
            ORIGINAL_MODELS.put(location, original);
            models.put(location, unknown);
            changed = true;
        }
        if (changed) {
            minecraft.getItemRenderer().onResourceManagerReload(minecraft.getResourceManager());
            LOGGER.debug("Updated {} masked item models from AStages", ORIGINAL_MODELS.size());
        }
        return true;
    }

    private static ModelResourceLocation inventoryModel(ResourceLocation id) {
        return new ModelResourceLocation(id, "inventory");
    }

    private static Field findBakedRegistry() {
        // NeoForge's model-bake event exposes a read-only view after baking.
        try {
            Field field = ModelManager.class.getDeclaredField("bakedRegistry");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException error) {
            LOGGER.error("Cannot access bakedRegistry", error);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<ModelResourceLocation, BakedModel> readBakedRegistry(ModelManager manager) {
        try {
            return (Map<ModelResourceLocation, BakedModel>) BAKED_REGISTRY.get(manager);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Cannot read bakedRegistry", error);
        }
    }
}
