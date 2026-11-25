package com.mysterious.mystic.compat.astages;

import com.alessandro.astages.api.constant.AStageType;
import com.alessandro.astages.api.holder.AClientHolder;
import com.alessandro.astages.api.wrapper.RecipeWrapper;
import com.alessandro.astages.engine.AClientRestrictionManager;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class ClientRecipeStageTooltips {
    private ClientRecipeStageTooltips() {
    }

    public static List<Component> forRecipe(RecipeHolder<?> recipe) {
        if (recipe == null || !clientReady()) return List.of();
        return hintsFor(recipeStages(recipe));
    }

    public static List<Component> forItem(ItemStack stack) {
        if (stack.isEmpty() || !clientReady()) return List.of();
        return hintsFor(itemStages(stack));
    }

    public static boolean isItemLocked(ItemStack stack) {
        if (stack.isEmpty() || !clientReady()) return false;
        return isLocked(itemStages(stack));
    }

    public static boolean isRecipeLocked(RecipeHolder<?> recipe) {
        if (recipe == null || !clientReady()) return false;
        return isLocked(recipeStages(recipe));
    }

    private static List<Component> hintsFor(StageRequirements required) {
        var stages = AClientHolder.serverAndPlayer().getStages();
        return required.hints(stages.getForType(AStageType.SERVER), stages.getForType(AStageType.PLAYER));
    }

    private static boolean isLocked(StageRequirements required) {
        var stages = AClientHolder.serverAndPlayer().getStages();
        return required.isLocked(stages.getForType(AStageType.SERVER), stages.getForType(AStageType.PLAYER));
    }

    private static boolean clientReady() {
        return Minecraft.getInstance().player != null && AClientRestrictionManager.areRestrictionsAvailable();
    }

    private static StageRequirements itemStages(ItemStack stack) {
        // getStagesForStack only includes rules that hide the entry, omitting showInRecipeViewer rules.
        Set<String> required = new TreeSet<>();
        var registry = AClientRestrictionManager.ITEM_INSTANCE.getRegistry();
        registry.forEach(restriction -> {
            if (restriction.isRestricted(stack)) required.add(restriction.getStage());
        });
        // The pack uses mod-wide recipe stages as chapter entry gates, including for items without recipes.
        // Keep this group separate from item-specific stages such as JDT's time_series.
        String modId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        Set<String> modStages = new TreeSet<>();
        for (var restriction : AClientRestrictionManager.RECIPE_INSTANCE.getRegistry().getModRestrictions()) {
            if (modId.equals(restriction.getModId())) modStages.add(restriction.getStage());
        }
        return new StageRequirements(modStages, required);
    }

    private static StageRequirements recipeStages(RecipeHolder<?> recipe) {
        var wrapper = new RecipeWrapper(recipe.value().getType(), recipe.id());
        var registry = AClientRestrictionManager.RECIPE_INSTANCE.getRegistry();
        Set<String> modStages = new TreeSet<>();
        for (var restriction : registry.getModRestrictions()) {
            if (restriction.isRestricted(wrapper)) modStages.add(restriction.getStage());
        }
        Set<String> recipeStages = new TreeSet<>();
        for (var restriction : registry.getRecipeRestrictions()) {
            if (restriction.isRestricted(wrapper)) recipeStages.add(restriction.getStage());
        }
        return new StageRequirements(modStages, recipeStages);
    }

    private static boolean hasMissingStages(Set<String> required, Set<String> server, Set<String> player) {
        return !server.containsAll(required) && !player.containsAll(required);
    }

    private static void addMissingStages(Set<String> required, Set<String> server, Set<String> player,
                                         Set<Component> lines) {
        if (!hasMissingStages(required, server, player)) return;
        Set<String> serverMissing = new TreeSet<>(required);
        serverMissing.removeAll(server);
        Set<String> playerMissing = new TreeSet<>(required);
        playerMissing.removeAll(player);
        Set<String> missingInBoth = new TreeSet<>(serverMissing);
        missingInBoth.retainAll(playerMissing);
        for (String stage : missingInBoth) {
            lines.add(Component.translatable("tooltip.eicore.recipe_stage.missing", stageName(stage))
                    .withStyle(ChatFormatting.RED));
        }
        serverMissing.removeAll(missingInBoth);
        playerMissing.removeAll(missingInBoth);
        if (!serverMissing.isEmpty() && !playerMissing.isEmpty()) {
            lines.add(Component.translatable("tooltip.eicore.recipe_stage.split",
                    ComponentUtils.formatList(serverMissing, ClientRecipeStageTooltips::stageName),
                    ComponentUtils.formatList(playerMissing, ClientRecipeStageTooltips::stageName))
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static Component stageName(String stage) {
        return Component.translatableWithFallback("stage.eicore." + stage, stage);
    }

    private record StageRequirements(Set<String> mod, Set<String> entry) {
        private boolean isLocked(Set<String> server, Set<String> player) {
            // Each independent restriction group accepts a complete server set or a complete player set.
            return hasMissingStages(mod, server, player) || hasMissingStages(entry, server, player);
        }

        private List<Component> hints(Set<String> server, Set<String> player) {
            Set<Component> lines = new LinkedHashSet<>();
            addMissingStages(mod, server, player, lines);
            addMissingStages(entry, server, player, lines);
            return List.copyOf(lines);
        }
    }
}
