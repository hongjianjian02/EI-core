package com.mysterious.mystic.scribe;

import com.mysterious.mystic.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public final class RitualScribeMenu extends AbstractContainerMenu {
    private final DataSlot state = addDataSlot(DataSlot.standalone());
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack openedStack;
    private final int selectedSlot;

    public RitualScribeMenu(int containerId, Inventory inventory) {
        this(containerId, inventory.player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    RitualScribeMenu(int containerId, Player owner, InteractionHand hand, ItemStack openedStack) {
        super(RitualScribe.MENU.get(), containerId);
        this.owner = owner;
        this.hand = hand;
        this.openedStack = openedStack;
        this.selectedSlot = owner.getInventory().selected;
        state.set(ScribeState.of(openedStack).pack());
    }

    public ScribeState state() {
        return ScribeState.unpack(state.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && (player.level().isClientSide || (openedStack.is(ModItems.RITUAL_SCRIBE_TOOL.get())
                && (hand == InteractionHand.OFF_HAND || player.getInventory().selected == selectedSlot)
                && player.getItemInHand(hand) == openedStack));
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.containerMenu != this || !stillValid(player)) return false;
        ScribeMode mode = ScribeMode.atIndex(buttonId);
        ScribeState current = ScribeState.of(openedStack);
        if (mode == null || !current.has(mode) || mode.tool().isEmpty()) return false;

        openedStack.set(RitualScribe.STATE.get(), current.select(mode));
        player.getInventory().setChanged();
        serverPlayer.closeContainer();
        serverPlayer.displayClientMessage(Component.translatable("message.eicore.ritual_scribe.selected", mode.label()), true);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
