package com.mysterious.mystic.foodbox;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class FlavorBoxItem extends Item {
    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();

    public FlavorBoxItem(Properties properties) {
        super(properties);
    }

    private static ItemContainerContents legacyStorage(ItemStack box) {
        return box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    private static FlavorBoxContents storage(ItemStack box) {
        FlavorBoxContents stored = box.get(FlavorBoxComponents.CONTENTS.get());
        if (stored != null) {
            return stored;
        }
        ItemContainerContents legacy = legacyStorage(box);
        return legacy.getSlots() == 0 ? FlavorBoxContents.EMPTY
                : new FlavorBoxContents(ImmutableList.copyOf(legacy.nonEmptyItems()));
    }

    static FlavorBoxContents editableStorage(ItemStack box) {
        FlavorBoxContents stored = storage(box);
        if (!isCurrent(box, stored)) {
            store(box, stored);
        }
        return stored;
    }

    static boolean isCurrent(ItemStack box, FlavorBoxContents expected) {
        return box.get(FlavorBoxComponents.CONTENTS.get()) == expected;
    }

    private static int storedCount(ItemStack box) {
        FlavorBoxContents stored = box.get(FlavorBoxComponents.CONTENTS.get());
        return stored == null ? Iterables.size(legacyStorage(box).nonEmptyItems()) : stored.size();
    }

    public static List<ItemStack> contents(ItemStack box) {
        return storage(box).copyItems();
    }

    public static void save(ItemStack box, List<ItemStack> contents) {
        store(box, new FlavorBoxContents(contents));
    }

    static void store(ItemStack box, FlavorBoxContents contents) {
        box.set(FlavorBoxComponents.CONTENTS.get(), contents);
        box.remove(DataComponents.CONTAINER);
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
        if (!level.isClientSide && !storage(box).anyServing(food -> FlavorFoodRules.canTaste(player, food))) {
            player.displayClientMessage(Component.translatable("message.eicore.flavor_box.no_food"), true);
            return InteractionResultHolder.fail(box);
        }
        if (level.isClientSide && storedCount(box) == 0) {
            return InteractionResultHolder.fail(box);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(box);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack box, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return box;
        }
        InteractionHand hand = player.getUsedItemHand();
        if (!holdingBox(player, level, hand, box) || !beginOperation(player)) {
            return player.getItemInHand(hand);
        }
        int eaten = 0;
        ItemStack currentFood = ItemStack.EMPTY;
        String phase = "preparing contents";
        try {
            FlavorBoxContents snapshot = editableStorage(box);
            var tasting = snapshot.beginTasting();
            while (tasting.hasNext() && canContinueTasting(player, level, hand, box, snapshot)) {
                phase = "checking eligibility";
                ItemStack food = tasting.serving();
                currentFood = food;
                if (food.getCount() != 1 || !FlavorFoodRules.canTaste(player, food)) {
                    tasting.skip();
                    continue;
                }
                phase = "starting use";
                int duration = food.getUseDuration(player);
                if (!canContinueTasting(player, level, hand, box, snapshot)) {
                    break;
                }
                int started = EventHooks.onItemUseStart(player, food, hand, duration);
                if (!canContinueTasting(player, level, hand, box, snapshot)) {
                    break;
                }
                if (started <= 0 || food.getCount() != 1 || !FlavorFoodRules.canTaste(player, food)) {
                    tasting.skip();
                    continue;
                }
                if (!canContinueTasting(player, level, hand, box, snapshot)) {
                    break;
                }
                ItemStack before = food.copy();
                currentFood = before;
                snapshot = tasting.take();
                store(box, snapshot);
                phase = "consuming food";
                ItemStack result;
                try {
                    result = food.finishUsingItem(level, player);
                } catch (RuntimeException exception) {
                    // The item may already have consumed or changed itself. Never recreate the pre-use copy.
                    player.getInventory().placeItemBackInInventory(food);
                    throw exception;
                }
                eaten++;
                phase = "finishing use";
                // Keep access to the last published remainder even if a later Finish listener throws.
                var finish = new LivingEntityUseItemEvent.Finish(player, before, 0, result);
                try {
                    NeoForge.EVENT_BUS.post(finish);
                } finally {
                    player.getInventory().placeItemBackInInventory(finish.getResultStack());
                }
            }
        } catch (RuntimeException exception) {
            LogUtils.getLogger().error("Flavor box stopped while {} for {} ({} consumed)", phase, currentFood, eaten, exception);
            player.displayClientMessage(Component.translatable("message.eicore.flavor_box.food_error"), false);
        } finally {
            try {
                player.getInventory().setChanged();
                player.inventoryMenu.broadcastChanges();
            } finally {
                endOperation(player);
            }
        }
        player.displayClientMessage(Component.translatable("message.eicore.flavor_box.tasted", eaten), true);
        return player.getItemInHand(hand);
    }

    private static boolean holdingBox(ServerPlayer player, Level level, InteractionHand hand, ItemStack box) {
        return player.isAlive() && !player.isRemoved() && !player.isSpectator() && player.level() == level
                && player.getItemInHand(hand) == box && box.getCount() == 1 && player.containerMenu == player.inventoryMenu;
    }

    private static boolean canContinueTasting(ServerPlayer player, Level level, InteractionHand hand, ItemStack box,
                                             FlavorBoxContents expected) {
        return holdingBox(player, level, hand, box) && isCurrent(box, expected);
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
        tooltip.add(Component.translatable("tooltip.eicore.flavor_box.contents", storedCount(stack))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.eicore.flavor_box.collect").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.eicore.flavor_box.taste").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.eicore.flavor_box.rules").withStyle(ChatFormatting.GRAY));
    }
}
