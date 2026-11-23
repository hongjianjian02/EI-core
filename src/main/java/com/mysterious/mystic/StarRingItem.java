package com.mysterious.mystic;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import top.theillusivec4.curios.api.CuriosApi;

public final class StarRingItem extends Item {
    private static final String RING_SLOT = "ring";
    private static final String READY_AT = "eicore.star_ring.ready_at";
    private static final int BONUS_PERCENT = 20;
    private static final int COOLDOWN_SECONDS = 10;
    private static final ThreadLocal<Strike> ACTIVE_STRIKE = new ThreadLocal<>();

    StarRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.eicore.star_ring", BONUS_PERCENT, COOLDOWN_SECONDS)
                .withStyle(ChatFormatting.GRAY));
    }

    public static boolean strike(Entity target, DamageSource source, float damage, Predicate<Float> hurt) {
        Entity victim = target instanceof PartEntity<?> part ? part.getParent() : target;
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
                || !source.is(DamageTypes.PLAYER_ATTACK) || ACTIVE_STRIKE.get() != null
                || !(victim instanceof LivingEntity living) || !(living instanceof Enemy)
                || damage <= 0.0F || gameTime(player) < cooldownData(player).getLong(READY_AT) || !isWorn(player)) {
            return hurt.test(damage);
        }

        ACTIVE_STRIKE.set(new Strike(player, living, source));
        try {
            return hurt.test(damage * (1.0F + BONUS_PERCENT / 100.0F));
        } finally {
            ACTIVE_STRIKE.remove();
        }
    }

    @SubscribeEvent
    public static void onDamageApplied(LivingDamageEvent.Post event) {
        Strike strike = ACTIVE_STRIKE.get();
        if (strike != null && strike.victim() == event.getEntity() && strike.source() == event.getSource()
                && event.getNewDamage() > 0.0F) {
            ServerPlayer player = strike.player();
            CompoundTag data = cooldownData(player);
            data.putLong(READY_AT, gameTime(player) + COOLDOWN_SECONDS * 20L);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
        }
    }

    private static CompoundTag cooldownData(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static long gameTime(ServerPlayer player) {
        return player.serverLevel().getServer().overworld().getGameTime();
    }

    private static boolean isWorn(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player).map(curios -> curios.getStacksHandler(RING_SLOT).map(slot -> {
            var stacks = slot.getStacks();
            for (int index = 0; index < stacks.getSlots(); index++) {
                if (curios.isSlotActive(RING_SLOT, index) && stacks.getStackInSlot(index).is(ModItems.STAR_RING.get())) {
                    return true;
                }
            }
            return false;
        }).orElse(false)).orElse(false);
    }

    private record Strike(ServerPlayer player, LivingEntity victim, DamageSource source) {
    }
}
