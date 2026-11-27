package com.mysterious.mystic.compat.anvilguide.jei;

import com.mysterious.mystic.compat.anvilguide.data.AnvilAddon;
import net.minecraft.network.chat.Component;

/**
 * 滚动网格里的一格：要么是附加物（附魔书 / 法术卷轴），要么是占位格，
 * 占位格里还可以带一条段落分隔带。
 *
 * @param addon  这一格放的附加物；占位格为 {@code null}
 * @param header 这一格画的分隔带标签；不是分隔带为 {@code null}
 * @param color  分隔带的文字颜色
 */
public record AnvilCell(AnvilAddon addon, Component header, int color) {

    public static AnvilCell of(AnvilAddon addon) {
        return new AnvilCell(addon, null, 0);
    }

    public static AnvilCell spacer() {
        return new AnvilCell(null, null, 0);
    }

    public static AnvilCell header(Component label, int color) {
        return new AnvilCell(null, label, color);
    }

    public boolean hasAddon() {
        return addon != null;
    }

    public boolean hasHeader() {
        return header != null;
    }
}
