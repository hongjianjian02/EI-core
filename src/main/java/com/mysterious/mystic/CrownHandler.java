package com.mysterious.mystic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.theillusivec4.curios.api.CuriosApi;
import twilightforest.components.entity.FortificationShieldAttachment;
import twilightforest.init.TFDataAttachments;

public final class CrownHandler {
    private static final ResourceLocation LUCK_ID =
            ResourceLocation.fromNamespaceAndPath(MysteriousMod.MODID, "crown_luck");
    private static final Map<UUID, Integer> SHIELD_COOLDOWN = new ConcurrentHashMap<>();

    private static final int SHIELD_AMOUNT = 3;
    private static final int SHIELD_COOLDOWN_SECONDS = 60;
    private static final int SHIELD_COOLDOWN_TICKS = SHIELD_COOLDOWN_SECONDS * 20;

    private CrownHandler() {
    }

    public static boolean wearingCrown(Player player) {
        try {
            return CuriosApi.getCuriosInventory(player)
                    .map(handler -> handler.findFirstCurio(ModItems.MYSTIC_CROWN.get()).isPresent())
                    .orElse(Boolean.FALSE)
                    .booleanValue();
        } catch (Throwable t) {
            return false;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();
        int cooldown = SHIELD_COOLDOWN.getOrDefault(uuid, 0);
        if (cooldown > 0) {
            SHIELD_COOLDOWN.put(uuid, cooldown - 1);
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        AttributeInstance luck = player.getAttribute(Attributes.LUCK);
        if (luck == null) {
            return;
        }

        boolean worn = wearingCrown(player);
        boolean hasModifier = luck.getModifier(LUCK_ID) != null;

        if (worn && !hasModifier) {
            luck.addTransientModifier(new AttributeModifier(LUCK_ID, 5.0D, AttributeModifier.Operation.ADD_VALUE));
        } else if (!worn && hasModifier) {
            luck.removeModifier(LUCK_ID);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        UUID uuid = player.getUUID();

        if (SHIELD_COOLDOWN.getOrDefault(uuid, 0) > 0) {
            return;
        }
        if (player.getHealth() > player.getMaxHealth() * 0.5F) {
            return;
        }
        if (!wearingCrown(player)) {
            return;
        }

        try {
            FortificationShieldAttachment shields = player.getData(TFDataAttachments.FORTIFICATION_SHIELDS);
            if (shields.shieldsLeft() > 0) {
                return;
            }
            shields.addShields(player, SHIELD_AMOUNT, true);
        } catch (Throwable t) {
            return;
        }

        SHIELD_COOLDOWN.put(uuid, SHIELD_COOLDOWN_TICKS);

        // 物品冷却条：在物品图标上显示倒计时
        player.getCooldowns().addCooldown(ModItems.MYSTIC_CROWN.get(), SHIELD_COOLDOWN_TICKS);

        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 1.6F);
    }
}