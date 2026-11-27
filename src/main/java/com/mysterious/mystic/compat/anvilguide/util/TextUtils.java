package com.mysterious.mystic.compat.anvilguide.util;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;

/** 纯文本工具：罗马数字、附魔名列表等。 */
public final class TextUtils {

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private TextUtils() {
    }

    /** 把 1~3999 转成罗马数字；超出范围时退回阿拉伯数字，保证任何模组附魔等级都能显示。 */
    public static String roman(int number) {
        if (number <= 0 || number > 3999) {
            return String.valueOf(number);
        }
        StringBuilder builder = new StringBuilder();
        int remaining = number;
        for (int index = 0; index < ROMAN_VALUES.length; index++) {
            while (remaining >= ROMAN_VALUES[index]) {
                builder.append(ROMAN_SYMBOLS[index]);
                remaining -= ROMAN_VALUES[index];
            }
        }
        return builder.toString();
    }

    /** 附魔名（不带等级），用于冲突提示。 */
    public static String enchantmentName(Holder<Enchantment> enchantment) {
        return enchantment.value().description().getString();
    }

    /** 用“、”连接附魔名，超过 limit 个时补一句“等 N 项”。 */
    public static String enchantmentNames(List<Holder<Enchantment>> enchantments, int limit) {
        if (enchantments.isEmpty()) {
            return "";
        }
        int count = Math.min(enchantments.size(), Math.max(1, limit));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append('、');
            }
            builder.append(enchantmentName(enchantments.get(i)));
        }
        if (enchantments.size() > count) {
            builder.append(" 等 ").append(enchantments.size()).append(" 项");
        }
        return builder.toString();
    }

    /** 用「、」连接物品名，超过 limit 个时补一句「等 N 种」。 */
    public static Component itemNames(List<ItemStack> stacks, int limit) {
        MutableComponent result = Component.empty();
        if (stacks.isEmpty()) {
            return result;
        }
        int count = Math.min(stacks.size(), Math.max(1, limit));
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                result.append("、");
            }
            result.append(stacks.get(i).getHoverName());
        }
        if (stacks.size() > count) {
            result.append(Component.translatable("eicore.anvil_guide.names.more", stacks.size()));
        }
        return result;
    }

    public static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
