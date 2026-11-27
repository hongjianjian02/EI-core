package com.mysterious.mystic.compat.anvilguide.emi;

import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** EMI geometry and stack presentation; shared guide text has no browser dependencies. */
final class EmiAnvilView {
    static final int WIDTH = 184;
    static final int ITEM_HEIGHT = 124;
    static final int BOOK_HEIGHT = 114;
    static final int REPAIR_HEIGHT = 106;
    static final int COLUMNS = 6;
    static final int ROWS = 4;
    static final int SLOT_SIZE = 18;
    static final int GRID_X = 54;
    static final int GRID_Y = 6;
    static final int GRID_WIDTH = COLUMNS * SLOT_SIZE;
    static final int GRID_HEIGHT = ROWS * SLOT_SIZE;
    static final int INPUT_X = 5;
    static final int INPUT_Y = 19;
    static final int TEXT_X = 6;
    static final int STATS_Y = 82;
    static final int HINT_Y = 92;
    static final int TEXT_LINE_HEIGHT = 10;
    static final int GRID_FILL = 0xFFF1ECFB;
    static final int GRID_BORDER = 0xFFC6B8E6;
    static final int BUTTON_FILL = 0xFFE8DFFA;
    static final int BUTTON_HOVER = 0xFFD6C8F4;
    static final int BUTTON_BORDER = 0xFF9F8DCC;
    static final int TEXT = 0xFF3A3054;
    static final int DIM_TEXT = 0xFF6C6486;
    static final int CURSE_TEXT = 0xFF9A3A5A;
    static final int DIVIDER = 0xFFBDAEDB;

    private EmiAnvilView() {
    }

    static EmiStack stack(ItemStack stack) {
        if (stack.isEmpty()) {
            return EmiStack.EMPTY;
        }
        return EmiStack.of(stack.copy()).comparison(Comparison.compareComponents());
    }

    static void addArrow(WidgetHolder widgets) {
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 28, 21);
    }

    static void panel(GuiGraphics graphics, int x, int y, int width, int height, int fill, int border) {
        graphics.fill(x + 1, y, x + width - 1, y + height, fill);
        graphics.fill(x, y + 1, x + width, y + height - 1, fill);
        graphics.fill(x + 1, y, x + width - 1, y + 1, border);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, border);
        graphics.fill(x, y + 1, x + 1, y + height - 1, border);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, border);
    }
}
