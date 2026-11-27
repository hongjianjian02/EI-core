package com.mysterious.mystic.compat.anvilguide.emi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRecipeFactory;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRepairRecipe;
import com.mysterious.mystic.compat.anvilguide.util.QueryResults;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The locked EMI manager hook uses the actual hovered stack, including late inventory variants. */
public final class FocusedEmiRecipes {
    private static final int CACHE_LIMIT = 256;
    private static final Map<StackKey, List<EmiRecipe>> CACHE = new LinkedHashMap<>(32, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<StackKey, List<EmiRecipe>> eldest) {
            return size() > CACHE_LIMIT;
        }
    };
    private static int generation = -1;

    private FocusedEmiRecipes() {
    }

    public static List<EmiRecipe> merge(EmiStack stack, List<EmiRecipe> original, boolean uses) {
        if (!AnvilEnchantEmiPlugin.canQuery()) {
            // EMI history and old managers may retain our pages while a world reload
            // is in progress. Do not expose that snapshot through R/U again.
            return QueryResults.replace(original, FocusedEmiRecipes::isOwnRecipe, List.of());
        }
        ItemStack item = stack.getItemStack();
        if (item.isEmpty()) {
            return original;
        }
        try {
            item = AnvilEnchantData.rebindEnchantments(item);
            // Observe real late books without inventing the intermediate levels up to 255.
            for (var type : List.of(DataComponents.ENCHANTMENTS, DataComponents.STORED_ENCHANTMENTS)) {
                for (var entry : item.getOrDefault(type, ItemEnchantments.EMPTY).entrySet()) {
                    AnvilEnchantData.observeBookLevel(entry.getKey(), entry.getIntValue());
                }
            }
            int currentGeneration = AnvilEnchantData.generation();
            if (currentGeneration != generation) {
                clear();
                AnvilRecipeFactory.invalidateCache();
                generation = currentGeneration;
            }
            ItemStack current = item;
            List<EmiRecipe> focused = CACHE.computeIfAbsent(new StackKey(item, uses),
                    ignored -> createFocused(current, uses));
            return replaceFocusedRecipes(original, focused, uses);
        } catch (RuntimeException | LinkageError exception) {
            AnvilEnchantEmiPlugin.LOGGER.debug("跳过当前连接无法解析的 EMI 铁砧查询堆叠", exception);
            // Do not return old native pages for an unresolved old-world enchantment.
            return replaceFocusedRecipes(original, List.of(), uses);
        }
    }

    private static List<EmiRecipe> replaceFocusedRecipes(List<EmiRecipe> original, List<EmiRecipe> focused,
            boolean uses) {
        return QueryResults.replace(original, recipe -> recipe instanceof EmiAnvilItemRecipe
                || recipe instanceof EmiAnvilBookRecipe || (uses && recipe instanceof EmiAnvilRepairRecipe), focused);
    }

    private static List<EmiRecipe> createFocused(ItemStack stack, boolean uses) {
        List<EmiRecipe> recipes = new ArrayList<>(2);
        // Transient pages have no serializable ID; favorites cannot restore an obsolete world Holder.
        if (AnvilEnchantData.isAnvilBook(stack)) {
            var model = AnvilRecipeFactory.bookRecipe(stack);
            if (!model.items().isEmpty()) {
                recipes.add(new EmiAnvilBookRecipe(null, model));
            }
        } else {
            var model = AnvilRecipeFactory.itemRecipe(stack, false);
            if (AnvilRecipeFactory.hasItemInformation(model)) {
                recipes.add(new EmiAnvilItemRecipe(null, model));
            }
        }
        if (uses) {
            List<ItemStack> targets = AnvilEnchantData.repairableItems(stack);
            if (!targets.isEmpty()) {
                var group = AnvilEnchantData.repairGroups().stream()
                        .filter(candidate -> candidate.materials().stream()
                                .anyMatch(material -> ItemStack.isSameItemSameComponents(material, stack)))
                        .filter(candidate -> sameTargets(candidate.items(), targets)).findFirst();
                List<ItemStack> materials = group.map(AnvilEnchantData.RepairGroup::materials)
                        .orElseGet(() -> List.of(stack.copyWithCount(1)));
                recipes.add(new EmiAnvilRepairRecipe(null, new AnvilRepairRecipe(materials, targets)));
            }
        }
        return List.copyOf(recipes);
    }

    private static boolean sameTargets(List<ItemStack> first, List<ItemStack> second) {
        return first.size() == second.size() && first.stream().allMatch(target -> second.stream()
                .anyMatch(other -> ItemStack.isSameItemSameComponents(target, other)));
    }

    public static void clear() {
        CACHE.clear();
        generation = -1;
    }

    private static boolean isOwnRecipe(EmiRecipe recipe) {
        return recipe instanceof EmiAnvilItemRecipe || recipe instanceof EmiAnvilBookRecipe
                || recipe instanceof EmiAnvilRepairRecipe;
    }

    private record StackKey(Item item, DataComponentPatch components, int count, boolean uses) {
        StackKey(ItemStack stack, boolean uses) {
            this(stack.getItem(), stack.getComponentsPatch(), stack.getCount(), uses);
        }
    }
}
