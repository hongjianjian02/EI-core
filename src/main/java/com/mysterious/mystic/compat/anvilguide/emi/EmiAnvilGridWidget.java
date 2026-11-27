package com.mysterious.mystic.compat.anvilguide.emi;

import java.util.List;

import com.mysterious.mystic.compat.anvilguide.util.TextUtils;
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
import net.minecraft.world.item.ItemStack;

/** A fixed 6×4 grid. Each visible cell remains an EMI slot for mouse and R/U lookup. */
public final class EmiAnvilGridWidget extends Widget {
    private static EmiAnvilGridWidget hovered;
    private static EmiAnvilGridWidget dragging;
    private static double screenMouseX;
    private static double screenMouseY;
    private static long frame;

    private final List<Cell> cells;
    private final int totalRows;
    private final int maxRow;
    private int firstRow;
    private double scrollRemainder;
    private double originX;
    private double originY;
    private double thumbGrabY;
    private long renderedFrame;

    EmiAnvilGridWidget(List<Cell> cells) {
        this.cells = List.copyOf(cells);
        this.totalRows = (cells.size() + EmiAnvilView.COLUMNS - 1) / EmiAnvilView.COLUMNS;
        this.maxRow = Math.max(0, totalRows - EmiAnvilView.ROWS);
    }

    static void add(WidgetHolder widgets, List<Cell> cells) {
        EmiAnvilGridWidget grid = widgets.add(new EmiAnvilGridWidget(cells));
        for (int index = 0; index < EmiAnvilView.COLUMNS * EmiAnvilView.ROWS; index++) {
            widgets.add(grid.new GridSlot(index));
        }
        if (grid.maxRow > 0) {
            widgets.add(grid.new ScrollbarWidget());
        }
    }

    /** Called before the recipe screen renders; prevents a previous page capturing scroll input. */
    public static void beginFrame(double mouseX, double mouseY) {
        hovered = null;
        screenMouseX = mouseX;
        screenMouseY = mouseY;
        frame++;
    }

    public static boolean scrollHovered(double mouseX, double mouseY, double delta) {
        EmiAnvilGridWidget grid = hovered;
        if (grid == null || grid.maxRow == 0 || !grid.containsScreen(mouseX, mouseY)) {
            return false;
        }
        grid.scrollRemainder += delta;
        int rows = (int) grid.scrollRemainder;
        grid.scrollRemainder -= rows;
        grid.firstRow = Math.clamp(grid.firstRow - rows, 0, grid.maxRow);
        // Keep wheel input inside this grid even at its limits, rather than changing EMI pages.
        return true;
    }

    public static boolean dragScrollbar(double mouseY) {
        if (dragging == null || dragging.renderedFrame != frame) {
            dragging = null;
            return false;
        }
        dragging.setThumbTop(mouseY - dragging.originY - dragging.thumbGrabY);
        return true;
    }

    public static void releaseScrollbar() {
        dragging = null;
    }

    public static void clearInteraction() {
        hovered = null;
        dragging = null;
    }

    private boolean containsScreen(double mouseX, double mouseY) {
        return TextUtils.isInside(mouseX - originX, mouseY - originY, EmiAnvilView.GRID_X - 3,
                EmiAnvilView.GRID_Y - 3, EmiAnvilView.GRID_WIDTH + (maxRow > 0 ? 17 : 6),
                EmiAnvilView.GRID_HEIGHT + 6);
    }

    @Override
    public Bounds getBounds() {
        // This background must not steal tooltip or key events from its individual SlotWidgets.
        return Bounds.EMPTY;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderedFrame = frame;
        originX = screenMouseX - mouseX;
        originY = screenMouseY - mouseY;
        if (containsScreen(screenMouseX, screenMouseY)) {
            hovered = this;
        }
        EmiAnvilView.panel(graphics, EmiAnvilView.GRID_X - 3, EmiAnvilView.GRID_Y - 3,
                EmiAnvilView.GRID_WIDTH + (maxRow > 0 ? 17 : 6), EmiAnvilView.GRID_HEIGHT + 6,
                EmiAnvilView.GRID_FILL, EmiAnvilView.GRID_BORDER);
        for (int index = 0; index < EmiAnvilView.COLUMNS * EmiAnvilView.ROWS; index++) {
            Cell cell = visibleCell(index);
            if (cell.header() != null) {
                drawHeader(graphics, cell.header(), EmiAnvilView.GRID_Y
                        + (index / EmiAnvilView.COLUMNS) * EmiAnvilView.SLOT_SIZE);
            }
        }
    }

    private void drawHeader(GuiGraphics graphics, Component header, int y) {
        Font font = Minecraft.getInstance().font;
        int available = EmiAnvilView.GRID_WIDTH - 4;
        String text = header.getString();
        if (font.width(text) > available) {
            text = font.plainSubstrByWidth(text, available - 6) + "…";
        }
        int textWidth = font.width(text);
        int textX = EmiAnvilView.GRID_X + (EmiAnvilView.GRID_WIDTH - textWidth) / 2;
        int lineY = y + EmiAnvilView.SLOT_SIZE / 2;
        if (textX > EmiAnvilView.GRID_X + 6) {
            graphics.fill(EmiAnvilView.GRID_X + 3, lineY, textX - 4, lineY + 1, EmiAnvilView.DIVIDER);
            graphics.fill(textX + textWidth + 4, lineY, EmiAnvilView.GRID_X + EmiAnvilView.GRID_WIDTH - 3,
                    lineY + 1, EmiAnvilView.DIVIDER);
        }
        graphics.drawString(font, text, textX, y + 5, EmiAnvilView.CURSE_TEXT, false);
    }

    private Cell visibleCell(int index) {
        int actual = firstRow * EmiAnvilView.COLUMNS + index;
        return actual < cells.size() ? cells.get(actual) : Cell.EMPTY;
    }

    private int thumbHeight() {
        return Math.max(12, EmiAnvilView.GRID_HEIGHT * EmiAnvilView.ROWS / totalRows);
    }

    private int thumbTop() {
        return EmiAnvilView.GRID_Y + firstRow * (EmiAnvilView.GRID_HEIGHT - thumbHeight()) / maxRow;
    }

    private void setThumbTop(double y) {
        double fraction = (y - EmiAnvilView.GRID_Y) / (EmiAnvilView.GRID_HEIGHT - thumbHeight());
        firstRow = Math.clamp((int) Math.round(fraction * maxRow), 0, maxRow);
        scrollRemainder = 0;
    }

    private final class GridSlot extends SlotWidget {
        private final int index;

        GridSlot(int index) {
            super(EmiStack.EMPTY, EmiAnvilView.GRID_X + (index % EmiAnvilView.COLUMNS) * EmiAnvilView.SLOT_SIZE,
                    EmiAnvilView.GRID_Y + (index / EmiAnvilView.COLUMNS) * EmiAnvilView.SLOT_SIZE);
            this.index = index;
        }

        @Override
        public EmiIngredient getStack() {
            return visibleCell(index).stack();
        }

        @Override
        public Bounds getBounds() {
            return getStack().isEmpty() ? Bounds.EMPTY : super.getBounds();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (getStack().isEmpty()) {
                return;
            }
            super.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public void drawOverlay(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.drawOverlay(graphics, mouseX, mouseY, delta);
            Cell cell = visibleCell(index);
            if (cell.level() > 0) {
                drawLevel(graphics, cell.level(), x + 1, y + 1);
            }
            if (cell.conflict()) {
                graphics.fill(x + 1, y + 1, x + 7, y + 7, 0xE0A81A12);
                graphics.pose().pushPose();
                graphics.pose().translate(x + 2, y + 1, 1);
                graphics.pose().scale(0.5F, 0.5F, 1);
                graphics.drawString(Minecraft.getInstance().font, "!", 0, 0, 0xFFFFFFFF, true);
                graphics.pose().popPose();
            }
        }

        @Override
        protected void addSlotTooltip(List<ClientTooltipComponent> list) {
            super.addSlotTooltip(list);
            for (Component line : visibleCell(index).tooltip()) {
                list.add(ClientTooltipComponent.create(line.getVisualOrderText()));
            }
        }
    }

    private static void drawLevel(GuiGraphics graphics, int level, int x, int y) {
        Font font = Minecraft.getInstance().font;
        String text = TextUtils.roman(level);
        int width = Math.max(1, font.width(text));
        float scale = Math.min(1.0F, 15.0F / width);
        graphics.pose().pushPose();
        // Match vanilla item decorations: item models are drawn at z=150.
        graphics.pose().translate(x + 16 - width * scale, y + 16 - 9 * scale, 200.0F);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    private final class ScrollbarWidget extends Widget {
        @Override
        public Bounds getBounds() {
            return new Bounds(EmiAnvilView.GRID_X + EmiAnvilView.GRID_WIDTH + 3, EmiAnvilView.GRID_Y,
                    8, EmiAnvilView.GRID_HEIGHT);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            Bounds bounds = getBounds();
            graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), EmiAnvilView.GRID_BORDER);
            EmiAnvilView.panel(graphics, bounds.x(), thumbTop(), bounds.width(), thumbHeight(),
                    bounds.contains(mouseX, mouseY) ? EmiAnvilView.BUTTON_HOVER : EmiAnvilView.BUTTON_FILL,
                    EmiAnvilView.BUTTON_BORDER);
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (button != 0 || !getBounds().contains(mouseX, mouseY)) {
                return false;
            }
            if (mouseY >= thumbTop() && mouseY < thumbTop() + thumbHeight()) {
                thumbGrabY = mouseY - thumbTop();
            } else {
                thumbGrabY = thumbHeight() / 2.0;
                setThumbTop(mouseY - thumbGrabY);
            }
            dragging = EmiAnvilGridWidget.this;
            return true;
        }
    }

    record Cell(EmiStack stack, int level, boolean conflict, Component header, List<Component> tooltip) {
        static final Cell EMPTY = new Cell(EmiStack.EMPTY, 0, false, null, List.of());

        static Cell of(ItemStack stack, int level, boolean conflict, List<Component> tooltip) {
            return new Cell(EmiAnvilView.stack(stack), level, conflict, null, List.copyOf(tooltip));
        }

        static Cell header(Component text) {
            return new Cell(EmiStack.EMPTY, 0, false, text, List.of());
        }
    }
}
