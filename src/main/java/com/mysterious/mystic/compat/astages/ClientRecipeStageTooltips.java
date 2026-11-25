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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class ClientRecipeStageTooltips {
    private ClientRecipeStageTooltips() {
    }

    public static List<Component> forRecipe(RecipeHolder<?> recipe) {
        if (recipe == null || !clientReady()) return List.of();
        var required = recipeStages(recipe);
        var stages = AClientHolder.serverAndPlayer().getStages();
        var server = stages.getForType(AStageType.SERVER);
        var player = stages.getForType(AStageType.PLAYER);
        Set<Component> lines = new LinkedHashSet<>();
        // AStages 2.5.3 evaluates mod-wide rules and the per-recipe cache separately.
        // For each group, its server evaluator accepts a complete server set or a complete player set.
        addMissingStages(required.mod(), server, player, lines);
        addMissingStages(required.recipe(), server, player, lines);
        return List.copyOf(lines);
    }

    public static List<Component> forItem(ItemStack stack) {
        if (stack.isEmpty() || !clientReady()) return List.of();
        var required = itemStages(stack);
        var stages = AClientHolder.serverAndPlayer().getStages();
        Set<Component> lines = new LinkedHashSet<>();
        addMissingStages(required, stages.getForType(AStageType.SERVER), stages.getForType(AStageType.PLAYER), lines);
        return List.copyOf(lines);
    }

    public static boolean isItemLocked(ItemStack stack) {
        if (stack.isEmpty() || !clientReady()) return false;
        var stages = AClientHolder.serverAndPlayer().getStages();
        return hasMissingStages(itemStages(stack), stages.getForType(AStageType.SERVER),
                stages.getForType(AStageType.PLAYER));
    }

    public static boolean isRecipeLocked(RecipeHolder<?> recipe) {
        if (recipe == null || !clientReady()) return false;
        var required = recipeStages(recipe);
        var stages = AClientHolder.serverAndPlayer().getStages();
        var server = stages.getForType(AStageType.SERVER);
        var player = stages.getForType(AStageType.PLAYER);
        return hasMissingStages(required.mod(), server, player)
                || hasMissingStages(required.recipe(), server, player);
    }

    private static boolean clientReady() {
        return Minecraft.getInstance().player != null && AClientRestrictionManager.areRestrictionsAvailable();
    }

    private static Set<String> itemStages(ItemStack stack) {
        // getStagesForStack only includes rules that hide the entry, omitting showInRecipeViewer rules.
        Set<String> required = new TreeSet<>();
        var registry = AClientRestrictionManager.ITEM_INSTANCE.getRegistry();
        registry.forEach(restriction -> {
            if (restriction.isRestricted(stack)) required.add(restriction.getStage());
        });
        return required;
    }

    private static RecipeStages recipeStages(RecipeHolder<?> recipe) {
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
        return new RecipeStages(modStages, recipeStages);
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

    private record RecipeStages(Set<String> mod, Set<String> recipe) {
    }
}
