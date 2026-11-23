package com.mysterious.mystic.client;

import com.mysterious.mystic.scribe.RitualScribeMenu;
import com.mysterious.mystic.scribe.ScribeMode;
import com.mysterious.mystic.scribe.ScribeState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class RitualScribeScreen extends AbstractContainerScreen<RitualScribeMenu> {
    private static final int TILE_SIZE = 24;
    private static final int TILE_GAP = 4;
    private static final int ICON_SIZE = 16;
    private final List<ModeButton> buttons = new ArrayList<>();

    public RitualScribeScreen(RitualScribeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ScribeMode.MODES.size() * (TILE_SIZE + TILE_GAP) - TILE_GAP;
        imageHeight = TILE_SIZE;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        for (ScribeMode mode : ScribeMode.MODES) {
            int tileLeft = leftPos + mode.ordinal() * (TILE_SIZE + TILE_GAP);
            ModeButton button = new ModeButton(tileLeft, topPos, mode);
            buttons.add(addRenderableWidget(button));
        }
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateButtons();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void updateButtons() {
        ScribeState state = menu.state();
        buttons.forEach(button -> button.update(state));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private final class ModeButton extends Button {
        private final ScribeMode mode;
        private final ItemStack icon;
        private final Tooltip modeTip;
        private final Tooltip lockedTip;
        private final Tooltip selectedTip;
        private boolean selected;

        ModeButton(int left, int top, ScribeMode mode) {
            super(left, top, TILE_SIZE, TILE_SIZE, mode.label(), button -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, mode.ordinal());
                }
            }, DEFAULT_NARRATION);
            this.mode = mode;
            this.icon = mode.tool();
            this.modeTip = Tooltip.create(mode.label());
            this.selectedTip = Tooltip.create(mode.label().copy().append("\n")
                    .append(Component.translatable("screen.eicore.ritual_scribe.selected")));
            this.lockedTip = Tooltip.create(mode.label().copy().append("\n")
                    .append(Component.translatable("screen.eicore.ritual_scribe.locked", icon.getHoverName())));
        }

        void update(ScribeState state) {
            active = !icon.isEmpty() && state.has(mode);
            selected = active && state.selected() == mode;
            setTooltip(active ? (selected ? selectedTip : modeTip) : lockedTip);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int left = getX();
            int top = getY();
            boolean highlighted = isHoveredOrFocused();
            int background = highlighted ? 0x80626473 : 0x40202532;
            graphics.fill(left, top, left + width, top + height, background);
            if (selected) {
                graphics.renderOutline(left, top, width, height, 0xffffd66e);
            } else if (highlighted) {
                graphics.renderOutline(left, top, width, height, 0x90e3dfd7);
            }
            int iconLeft = left + (width - ICON_SIZE) / 2;
            int iconTop = top + (height - ICON_SIZE) / 2;
            if (!active) graphics.setColor(0.53F, 0.53F, 0.53F, 1.0F);
            graphics.renderFakeItem(icon, iconLeft, iconTop);
            if (!active) {
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                renderLock(graphics, left + width - 7, top + height - 8);
                graphics.pose().popPose();
            }
        }

        private void renderLock(GuiGraphics graphics, int left, int top) {
            int lock = 0xffc9c9cc;
            graphics.renderOutline(left + 1, top, 3, 4, lock);
            graphics.fill(left, top + 3, left + 5, top + 7, lock);
            graphics.fill(left + 2, top + 4, left + 3, top + 6, 0xff3a3542);
        }
    }
}
