package com.mysterious.mystic.scribe;

import com.mysterious.mystic.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class RitualScribeUpgradeRecipe extends CustomRecipe {
    public RitualScribeUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return readUpgrade(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Upgrade upgrade = readUpgrade(input);
        if (upgrade == null) return ItemStack.EMPTY;

        ItemStack output = upgrade.scribe().copyWithCount(1);
        output.set(RitualScribe.STATE.get(), ScribeState.of(output).unlock(upgrade.mode()));
        return output;
    }

    private static Upgrade readUpgrade(CraftingInput input) {
        if (input.ingredientCount() != 2) return null;

        ItemStack scribe = ItemStack.EMPTY;
        ScribeMode mode = null;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.RITUAL_SCRIBE_TOOL.get())) {
                if (!scribe.isEmpty()) return null;
                scribe = stack;
            } else {
                if (mode != null) return null;
                mode = ScribeMode.forTool(stack);
                if (mode == null) return null;
            }
        }

        return scribe.isEmpty() || mode == null || ScribeState.of(scribe).has(mode) ? null : new Upgrade(scribe, mode);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RitualScribe.UPGRADE.get();
    }

    private record Upgrade(ItemStack scribe, ScribeMode mode) {
    }
}
