package com.mysterious.mystic.foodbox;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class FlavorBoxCollection {
    private static final Set<BlockEntity> ACTIVE_CONTAINERS = Collections.newSetFromMap(new IdentityHashMap<>());

    private FlavorBoxCollection() {
    }

    static void collect(ServerPlayer player, ItemStack box, UseOnContext context) {
        if (!ready(player, box, context) || !FlavorBoxItem.beginOperation(player)) {
            return;
        }
        FlavorBoxContainers.Source source = null;
        boolean locked = false;
        int added = 0;
        ItemStack pending = ItemStack.EMPTY;
        try {
            source = FlavorBoxContainers.find(player, context.getHand(), context.getClickedPos(), context.getClickedFace());
            if (source == null) {
                message(player, "no_container");
                return;
            }
            if (source.blocks().stream().anyMatch(ACTIVE_CONTAINERS::contains)) {
                return;
            }
            ACTIVE_CONTAINERS.addAll(source.blocks());
            locked = true;
            IItemHandler handler = source.handler();
            Set<Item> stored = new HashSet<>();
            FlavorBoxItem.contents(box).stream().filter(food -> !food.isEmpty()).forEach(food -> stored.add(food.getItem()));
            int slots = handler.getSlots();
            for (int slot = 0; slot < slots; slot++) {
                if (!validSource(player, box, context, source) || handler.getSlots() != slots) {
                    break;
                }
                FlavorBoxContents snapshot = FlavorBoxItem.storage(box);
                List<ItemStack> contents = snapshot.copyItems();
                ItemStack before = handler.getStackInSlot(slot).copy();
                if (source.isInfinite().test(slot) || before.getCount() == Integer.MAX_VALUE
                        || stored.contains(before.getItem()) || !FlavorFoodRules.canTaste(player, before)) {
                    continue;
                }
                ItemStack simulated = handler.extractItem(slot, 1, true);
                if (simulated.getCount() != 1 || !ItemStack.isSameItemSameComponents(before, simulated)
                        || !FlavorFoodRules.canTaste(player, simulated)) {
                    continue;
                }
                if (!validSource(player, box, context, source) || source.isInfinite().test(slot)
                        || !snapshot.equals(FlavorBoxItem.storage(box))
                        || !ItemStack.matches(before, handler.getStackInSlot(slot))) {
                    break;
                }
                try {
                    pending = handler.extractItem(slot, 1, false).copy();
                } catch (RuntimeException exception) {
                    ItemStack expectedRemainder = before.copy();
                    expectedRemainder.shrink(1);
                    if (!source.isInfinite().test(slot)
                            && ItemStack.matches(expectedRemainder, handler.getStackInSlot(slot))) {
                        pending = before.copyWithCount(1);
                    }
                    throw exception;
                }
                if (pending.isEmpty()) {
                    continue;
                }
                if (ItemStack.matches(before, handler.getStackInSlot(slot))) {
                    pending = ItemStack.EMPTY;
                    LogUtils.getLogger().warn("Flavor box ignored a non-depleting extraction at {} slot {}", context.getClickedPos(), slot);
                    break;
                }
                if (pending.getCount() != 1 || !ItemStack.isSameItemSameComponents(pending, simulated)
                        || !validSource(player, box, context, source)
                        || !snapshot.equals(FlavorBoxItem.storage(box))
                        || stored.contains(pending.getItem()) || !FlavorFoodRules.canTaste(player, pending)) {
                    break;
                }
                Item foodType = pending.getItem();
                contents.add(pending.copy());
                FlavorBoxItem.save(box, contents);
                pending = ItemStack.EMPTY;
                stored.add(foodType);
                added++;
            }
            message(player, added > 0 ? "collected" : "nothing_new", added);
        } catch (RuntimeException exception) {
            LogUtils.getLogger().error("Flavor box collection failed at {} ({} foods committed)", context.getClickedPos(), added, exception);
            message(player, "collection_error", added);
        } finally {
            try {
                if (!pending.isEmpty()) {
                    recover(player, pending);
                }
            } finally {
                if (locked) {
                    ACTIVE_CONTAINERS.removeAll(source.blocks());
                }
                FlavorBoxItem.endOperation(player);
                player.getCooldowns().addCooldown(box.getItem(), 5);
                player.getInventory().setChanged();
                player.inventoryMenu.broadcastChanges();
            }
        }
    }

    private static boolean ready(ServerPlayer player, ItemStack box, UseOnContext context) {
        return player.isAlive() && !player.isSpectator() && player.isShiftKeyDown() && !player.isUsingItem()
                && box.getCount() == 1 && player.getItemInHand(context.getHand()) == box
                && player.containerMenu == player.inventoryMenu;
    }

    private static boolean validSource(ServerPlayer player, ItemStack box, UseOnContext context, FlavorBoxContainers.Source source) {
        if (!ready(player, box, context)) {
            return false;
        }
        for (BlockEntity block : source.blocks()) {
            if (block.isRemoved() || player.serverLevel().getBlockEntity(block.getBlockPos()) != block
                    || !FlavorBoxContainers.permitted(player, context.getHand(), block.getBlockPos())) {
                return false;
            }
        }
        FlavorBoxContainers.Source current = FlavorBoxContainers.find(player, context.getHand(), context.getClickedPos(), context.getClickedFace());
        return current != null && (!source.stableHandler() || current.handler() == source.handler())
                && current.blocks().equals(source.blocks());
    }

    private static void recover(ServerPlayer player, ItemStack extracted) {
        ItemStack remaining = extracted.copy();
        if (!remaining.isEmpty() && !player.getInventory().add(remaining)) {
            player.drop(remaining, false);
        }
    }

    private static void message(ServerPlayer player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable("message.eicore.flavor_box." + key, arguments), true);
    }
}
