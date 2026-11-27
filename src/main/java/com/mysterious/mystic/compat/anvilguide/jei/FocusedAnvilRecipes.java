package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.List;

import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRecipeFactory;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.ItemStack;

/**
 * R/U 查询使用玩家聚焦的完整堆叠，每个查询只返回一张聚合页。
 * 两张展示分类使用 RENDER_ONLY，避免 JEI 默认组件归一化同时匹配多张索引页。
 * 分类与催化剂浏览仍使用预注册页面，不改变其他模组的 subtype 规则。
 */
final class FocusedAnvilRecipes implements IRecipeManagerPlugin {

    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        ItemStack stack = focusedStack(focus);
        if (stack.isEmpty()) {
            return List.of();
        }
        try {
            if (AnvilEnchantData.isAnvilBook(stack)) {
                return AnvilRecipeFactory.bookRecipe(stack).items().isEmpty()
                        ? List.of() : List.of(AnvilEnchantJeiPlugin.BOOK_ITEMS);
            }
            return AnvilEnchantData.highestBooks(stack).isEmpty()
                    ? List.of() : List.of(AnvilEnchantJeiPlugin.ITEM_ENCHANT);
        } catch (RuntimeException | LinkageError exception) {
            reportInvalidFocus(exception);
            return List.of();
        }
    }

    @Override
    public <T, V> List<T> getRecipes(IRecipeCategory<T> category, IFocus<V> focus) {
        ItemStack stack = focusedStack(focus);
        if (stack.isEmpty()) {
            return List.of();
        }
        try {
            RecipeType<T> type = category.getRecipeType();
            boolean book = AnvilEnchantData.isAnvilBook(stack);
            if (type.equals(AnvilEnchantJeiPlugin.BOOK_ITEMS) && book) {
                var recipe = AnvilRecipeFactory.bookRecipe(stack);
                return recipe.items().isEmpty() ? List.of() : List.of(type.getRecipeClass().cast(recipe));
            }
            if (type.equals(AnvilEnchantJeiPlugin.ITEM_ENCHANT) && !book) {
                var recipe = AnvilRecipeFactory.itemRecipe(stack, false);
                return recipe.books().isEmpty() ? List.of() : List.of(type.getRecipeClass().cast(recipe));
            }
        } catch (RuntimeException | LinkageError exception) {
            // JEI 会禁用抛异常的插件；单个旧服堆叠不能影响后续查询。
            reportInvalidFocus(exception);
        }
        return List.of();
    }

    @Override
    public <T> List<T> getRecipes(IRecipeCategory<T> category) {
        return List.of();
    }

    private static <V> ItemStack focusedStack(IFocus<V> focus) {
        if (focus.getRole() != RecipeIngredientRole.INPUT && focus.getRole() != RecipeIngredientRole.OUTPUT) {
            return ItemStack.EMPTY;
        }
        return focus.checkedCast(VanillaTypes.ITEM_STACK)
                .map(typed -> typed.getTypedValue().getIngredient()).orElse(ItemStack.EMPTY);
    }

    private static void reportInvalidFocus(Throwable exception) {
        AnvilEnchantJeiPlugin.LOGGER.debug("跳过当前连接无法解析的铁砧查询堆叠", exception);
    }
}
