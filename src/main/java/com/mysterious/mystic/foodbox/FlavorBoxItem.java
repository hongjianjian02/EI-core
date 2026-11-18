package com.mysterious.mystic.foodbox;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class FlavorBoxItem extends Item {
    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();

    public FlavorBoxItem(Properties properties) {
        super(properties);
    }

    static FlavorBoxContents storage(ItemStack box) {
        FlavorBoxContents stored = box.get(FlavorBoxComponents.CONTENTS.get());
        return stored != null ? stored : new FlavorBoxContents(
                box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().toList());
    }

    public static List<ItemStack> contents(ItemStack box) {
        return storage(box).copyItems();
    }

    public static void save(ItemStack box, List<ItemStack> contents) {
        box.set(FlavorBoxComponents.CONTENTS.get(), new FlavorBoxContents(contents));
        box.remove(DataComponents.CONTAINER);
    }

    private static boolean hasUntasted(Player player, ItemStack box) {
        for (ItemStack food : contents(box)) {
            if (food.getCount() == 1 && FlavorFoodRules.canTaste(player, food)) {
                return true;
            }
        }
        return false;
    }

    static boolean beginOperation(Player player) {
        return ACTIVE_PLAYERS.add(player.getUUID());
    }

    static void endOperation(Player player) {
        ACTIVE_PLAYERS.remove(player.getUUID());
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            FlavorBoxCollection.collect(serverPlayer, stack, context);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (player.isSpectator() || player.isShiftKeyDown()
                || !level.isClientSide && ACTIVE_PLAYERS.contains(player.getUUID())) {
            return InteractionResultHolder.fail(box);
        }
        if (!level.isClientSide && !hasUntasted(player, box)) {
            player.displayClientMessage(Component.translatable("message.eicore.flavor_box.no_food"), true);
            return InteractionResultHolder.fail(box);
        }
        if (contents(box).stream().allMatch(ItemStack::isEmpty)) {
            return InteractionResultHolder.fail(box);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(box);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack box, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                || player.getItemInHand(player.getUsedItemHand()) != box
                || player.containerMenu != player.inventoryMenu
                || !beginOperation(player)) {
            return box;
        }
        try {
            List<ItemStack> contents = contents(box);
            int eaten = 0;
            for (int slot = 0; slot < contents.size(); slot++) {
                if (!player.isAlive() || player.getItemInHand(player.getUsedItemHand()) != box
                        || player.containerMenu != player.inventoryMenu) {
                    break;
                }
                ItemStack food = contents.get(slot);
                if (food.getCount() != 1 || !FlavorFoodRules.canTaste(player, food)) {
                    continue;
                }
                if (EventHooks.onItemUseStart(player, food, player.getUsedItemHand(),
                        food.getUseDuration(player)) <= 0) {
                    continue;
                }
                if (!player.isAlive() || player.getItemInHand(player.getUsedItemHand()) != box
                        || player.containerMenu != player.inventoryMenu) {
                    break;
                }
                ItemStack before = food.copy();
                contents.set(slot, ItemStack.EMPTY);
                save(box, contents);
                try {
                    ItemStack result = food.finishUsingItem(level, player);
                    result = EventHooks.onItemUseFinish(player, before, 0, result);
                    if (ItemStack.isSameItem(result, before)) {
                        result.shrink(1);
                    }
                    if (!result.isEmpty() && !player.getInventory().add(result)) {
                        player.drop(result, false);
                    }
                    eaten++;
                } catch (RuntimeException exception) {
                    LogUtils.getLogger().error("Flavor box food consumption failed for {}", before, exception);
                    player.displayClientMessage(Component.translatable("message.eicore.flavor_box.food_error"), false);
                    break;
                }
            }
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            player.displayClientMessage(Component.translatable("message.eicore.flavor_box.tasted", eaten), true);
        } finally {
            endOperation(player);
        }
        return box;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 160;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long count = contents(stack).stream().filter(food -> !food.isEmpty()).count();
        tooltip.add(Component.translatable("tooltip.eicore.flavor_box.contents", count)
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.eicore.flavor_box.collect").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.eicore.flavor_box.taste").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.eicore.flavor_box.rules").withStyle(ChatFormatting.GRAY));
    }
}
