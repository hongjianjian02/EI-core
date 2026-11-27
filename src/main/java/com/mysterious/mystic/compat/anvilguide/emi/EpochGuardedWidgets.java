package com.mysterious.mystic.compat.anvilguide.emi;

import java.util.List;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Keeps widgets already retained by an EMI screen from using a previous connection's data. */
final class EpochGuardedWidgets implements WidgetHolder {
    private final WidgetHolder holder;
    private final long epoch;

    EpochGuardedWidgets(WidgetHolder holder, long epoch) {
        this.holder = holder;
        this.epoch = epoch;
        // The refresh message must survive invalidation, so it is added to the real holder directly.
        holder.add(new RefreshMessage());
    }

    boolean isCurrent() {
        return EmiViewEpoch.isCurrent(epoch) && AnvilEnchantEmiPlugin.canQuery();
    }

    @Override
    public int getWidth() {
        return holder.getWidth();
    }

    @Override
    public int getHeight() {
        return holder.getHeight();
    }

    @Override
    public <T extends Widget> T add(T widget) {
        if (widget instanceof SlotWidget slot) {
            holder.add(new GuardedSlot(slot));
        } else {
            holder.add(new GuardedWidget(widget));
        }
        // Chained tooltip, recipe-context and visual settings still mutate the original delegate.
        return widget;
    }

    private final class GuardedSlot extends SlotWidget {
        private final SlotWidget delegate;

        GuardedSlot(SlotWidget delegate) {
            super(EmiStack.EMPTY, 0, 0);
            this.delegate = delegate;
        }

        @Override
        public EmiIngredient getStack() {
            return isCurrent() ? delegate.getStack() : EmiStack.EMPTY;
        }

        @Override
        public EmiRecipe getRecipe() {
            return isCurrent() ? delegate.getRecipe() : null;
        }

        @Override
        public Bounds getBounds() {
            return isCurrent() ? delegate.getBounds() : Bounds.EMPTY;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (isCurrent()) {
                delegate.render(graphics, mouseX, mouseY, delta);
            }
        }

        @Override
        public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
            return isCurrent() ? delegate.getTooltip(mouseX, mouseY) : List.of();
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            return isCurrent() && delegate.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return isCurrent() && delegate.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    private final class GuardedWidget extends Widget {
        private final Widget delegate;

        GuardedWidget(Widget delegate) {
            this.delegate = delegate;
        }

        @Override
        public Bounds getBounds() {
            return isCurrent() ? delegate.getBounds() : Bounds.EMPTY;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (isCurrent()) {
                delegate.render(graphics, mouseX, mouseY, delta);
            }
        }

        @Override
        public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
            return isCurrent() ? delegate.getTooltip(mouseX, mouseY) : List.of();
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            return isCurrent() && delegate.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return isCurrent() && delegate.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    private final class RefreshMessage extends Widget {
        @Override
        public Bounds getBounds() {
            return Bounds.EMPTY;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (isCurrent()) {
                return;
            }
            Font font = Minecraft.getInstance().font;
            String message = EmiViewEpoch.isCurrent(epoch)
                    ? "eicore.anvil_guide.hint.refreshing" : "eicore.anvil_guide.hint.stale_page";
            List<FormattedCharSequence> lines = font.split(
                    Component.translatable(message), Math.max(1, getWidth() - 12));
            int y = Math.max(0, (getHeight() - lines.size() * EmiAnvilView.TEXT_LINE_HEIGHT) / 2);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            for (FormattedCharSequence line : lines) {
                graphics.drawCenteredString(font, line, getWidth() / 2, y, EmiAnvilView.DIM_TEXT);
                y += EmiAnvilView.TEXT_LINE_HEIGHT;
            }
            graphics.pose().popPose();
        }
    }
}
