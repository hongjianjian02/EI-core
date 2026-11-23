package com.mysterious.mystic.scribe;

import com.mysterious.mystic.ModItems;
import com.mysterious.mystic.MysteriousMod;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@EmiEntrypoint
public final class RitualScribeEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        if (registry.getRecipeManager().byKey(RitualScribe.UPGRADE_ID).isEmpty()) return;
        EmiStack base = EmiStack.of(ModItems.RITUAL_SCRIBE_TOOL.get());
        for (ScribeMode mode : ScribeMode.MODES) {
            ItemStack tool = mode.tool();
            if (tool.isEmpty()) continue;
            ItemStack result = new ItemStack(ModItems.RITUAL_SCRIBE_TOOL.get());
            result.set(RitualScribe.STATE.get(), ScribeState.EMPTY.unlock(mode));
            registry.addRecipe(new UpgradeDisplay(List.of(base, EmiStack.of(tool)), EmiStack.of(result),
                    ResourceLocation.fromNamespaceAndPath(MysteriousMod.MODID, "/ritual_scribe_upgrade/" + mode.key())));
        }
    }

    private static final class UpgradeDisplay extends EmiCraftingRecipe {
        UpgradeDisplay(List<EmiIngredient> inputs, EmiStack output, ResourceLocation id) {
            super(inputs, output, id, true);
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            super.addWidgets(widgets);
            widgets.addTooltipText(List.of(Component.translatable("emi.eicore.ritual_scribe.upgrade")), 60, 18, 24, 17);
        }
    }
}
