package com.mysterious.mystic.compat.anvilguide.ui;

import java.util.ArrayList;
import java.util.List;

import com.mysterious.mystic.compat.anvilguide.recipe.AnvilBookItemsRecipe;
import com.mysterious.mystic.compat.anvilguide.util.TextUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/** Client font layout shared by both browsers; positions and drawing stay with each view. */
public final class AnvilGuideLayoutText {
    private AnvilGuideLayoutText() {
    }

    public static List<Component> legendLines(List<AnvilBookItemsRecipe.GroupCount> groups, Font font, int maxWidth) {
        if (groups.isEmpty()) {
            return List.of(Component.translatable("eicore.anvil_guide.stats.none"));
        }
        List<Component> lines = new ArrayList<>();
        MutableComponent current = Component.empty();
        for (AnvilBookItemsRecipe.GroupCount group : groups) {
            Component part = Component.empty().append(group.group().displayName())
                    .append(Component.literal(" " + group.count()));
            if (current.getString().isEmpty()) {
                current.append(part);
                continue;
            }
            MutableComponent candidate = Component.empty().append(current).append(" · ").append(part);
            if (font.width(candidate) > maxWidth) {
                lines.add(current);
                current = Component.empty().append(part);
            } else {
                current = candidate;
            }
        }
        lines.add(current);
        return List.copyOf(lines);
    }

    /** Callers omit the names when the available width is at most eight pixels. */
    public static Component repairNames(List<ItemStack> materials, Font font, int maxWidth) {
        String names = TextUtils.itemNames(materials, 2).getString();
        if (font.width(names) > maxWidth) {
            names = font.plainSubstrByWidth(names, maxWidth - 6) + "…";
        }
        return Component.literal(names);
    }
}
