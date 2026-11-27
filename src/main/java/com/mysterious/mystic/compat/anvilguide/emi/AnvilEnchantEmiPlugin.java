package com.mysterious.mystic.compat.anvilguide.emi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mysterious.mystic.compat.anvilguide.AnvilGuide;
import com.mysterious.mystic.compat.anvilguide.data.AnvilBrowserData;
import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRecipeFactory;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRepairRecipe;
import com.mojang.logging.LogUtils;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import org.slf4j.Logger;

/** Optional client entrypoint. EMI owns the reload cycle; shared world data is built on the client thread. */
@EmiEntrypoint
public final class AnvilEnchantEmiPlugin implements EmiPlugin {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final EmiRecipeCategory ITEM_ENCHANT = category("emi_item_enchant", "item", Items.ENCHANTED_BOOK);
    public static final EmiRecipeCategory BOOK_ITEMS = category("emi_book_items", "book", Items.DIAMOND_SWORD);
    private static boolean listenersRegistered;
    private static volatile RegistryAccess activeRegistries;
    private static volatile Object activeConnection;

    private static EmiRecipeCategory category(String path, String title, Item icon) {
        return new EmiRecipeCategory(id(path), EmiStack.of(icon)) {
            @Override
            public Component getName() {
                return Component.translatable("eicore.anvil_guide.category." + title);
            }
        };
    }

    private static ResourceLocation id(String path) {
        return AnvilGuide.id(path);
    }

    static ResourceLocation recipeId(String kind, int index) {
        return id("/emi/" + kind + "/" + index);
    }

    @Override
    public void register(EmiRegistry registry) {
        if (!isSupportedVersion() || !(EmiApi.getRecipeManager() instanceof EmiFocusedQueryManager)) {
            LOGGER.warn("EMI 原生查询未启用：需要已核对的 {} 与完整查询 hook；请核对版本和 Mixin 加载日志",
                    EmiFocusedQueryManager.SUPPORTED_EMI_VERSION);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.submit(AnvilEnchantEmiPlugin::registerListeners).join();
        // Late registration includes stacks added by other plugins, after EMI has baked its item index.
        registry.addDeferredRecipes(addRecipe -> {
            // Vanilla initializes this mutable API field after plugin discovery. Read it here,
            // rather than freezing a potentially null category in a static initializer.
            EmiRecipeCategory repairCategory = VanillaEmiRecipeCategories.ANVIL_REPAIRING;
            if (repairCategory == null || !NativeAnvilRepairReplacement.CATEGORY_ID.equals(repairCategory.getId())) {
                LOGGER.warn("未找到已核对的 EMI 铁砧分类，跳过本次替代注册");
                return;
            }
            List<ItemStack> samples = EmiApi.getIndexStacks().stream().map(EmiStack::getItemStack)
                    .filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
            Snapshot snapshot = minecraft.submit(() -> buildSnapshot(samples)).join();
            if (snapshot.recipes().isEmpty() || !minecraft.submit(() -> snapshot.connection() == minecraft.getConnection()
                    && snapshot.registries() == currentRegistries()).join()) {
                return;
            }
            // The native anvil category and its three workstations are already registered by EMI.
            for (EmiRecipeCategory category : List.of(ITEM_ENCHANT, BOOK_ITEMS)) {
                registry.addCategory(category);
                for (Item anvil : List.of(Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL)) {
                    registry.addWorkstation(category, EmiStack.of(anvil));
                }
            }
            snapshot.recipes().forEach(addRecipe);
            // JEMI uses the exact JEI category ID. Only our replaced basic categories are removed.
            Set<ResourceLocation> replaced = new HashSet<>();
            if (snapshot.items() > 0) {
                replaced.add(id("item_enchant"));
            }
            if (snapshot.books() > 0) {
                replaced.add(id("book_items"));
            }
            if (snapshot.repairs() > 0) {
                replaced.add(id("anvil_repair"));
                Set<String> hiddenTypes = new HashSet<>();
                registry.removeRecipes(recipe -> {
                    if (!replacesNativeAnvilRecipe(recipe, snapshot.repairReplacement())) {
                        return false;
                    }
                    // The locked EMI bake evaluates this predicate serially. Log actual
                    // matches separately from registration, once per native list type.
                    if (hiddenTypes.add(recipe.getClass().getName())) {
                        LOGGER.info("已匹配并隐藏 EMI 原生铁砧旧页：类型 {}，首个配方 {}",
                                recipe.getClass().getName(), recipe.getId());
                    }
                    return true;
                });
                LOGGER.info("已在 EMI 原生铁砧分类启用 {} 组材料修补网格；启用原生材料、装备合并与附魔旧页隐藏规则",
                        snapshot.repairs());
            }
            registry.removeRecipes(recipe -> recipe.getClass().getName().equals("dev.emi.emi.jemi.JemiRecipe")
                    && replaced.contains(recipe.getCategory().getId()));
            LOGGER.info("EMI 原生铁砧视图已注册：{} 张页面（物品 {}、书 {}、修复材料组 {}）",
                    snapshot.recipes().size(), snapshot.items(), snapshot.books(), snapshot.repairs());
        });
    }

    private static boolean isSupportedVersion() {
        return ModList.get().getModContainerById("emi")
                .map(mod -> EmiFocusedQueryManager.SUPPORTED_EMI_VERSION.equals(mod.getModInfo().getVersion().toString()))
                .orElse(false);
    }

    private static boolean replacesNativeAnvilRecipe(EmiRecipe recipe, NativeAnvilRepairReplacement replacement) {
        String className = recipe.getClass().getName();
        if (!replacement.isNativeType(className)) {
            return false;
        }
        EmiRecipeCategory category = recipe.getCategory();
        return category != null && category == VanillaEmiRecipeCategories.ANVIL_REPAIRING
                && replacement.shouldHide(className, category.getId(), recipe.getId());
    }

    private static Snapshot buildSnapshot(List<ItemStack> samples) {
        Minecraft minecraft = Minecraft.getInstance();
        RegistryAccess access = currentRegistries();
        invalidateQuerySnapshot();
        if (access == null || access.registry(Registries.ENCHANTMENT).isEmpty()) {
            return Snapshot.EMPTY;
        }
        AnvilBrowserData.build("emi", access, samples);
        AnvilRecipeFactory.invalidateCache();
        List<EmiRecipe> recipes = new ArrayList<>();
        List<AnvilEnchantData.RepairGroup> repairGroups = AnvilEnchantData.repairGroups();
        List<ItemStack> itemCandidates = new ArrayList<>(AnvilEnchantData.enchantableStacks());
        for (var group : repairGroups) {
            itemCandidates.addAll(group.items());
        }
        Set<StackKey> seenItems = new HashSet<>();
        int items = 0;
        for (ItemStack stack : itemCandidates) {
            if (!seenItems.add(new StackKey(stack))) {
                continue;
            }
            var model = AnvilRecipeFactory.itemRecipe(stack, false);
            if (AnvilRecipeFactory.hasItemInformation(model)) {
                recipes.add(new EmiAnvilItemRecipe(recipeId("item", items++), model));
            }
        }
        int books = 0;
        Set<StackKey> seen = new HashSet<>();
        for (var enchantment : AnvilEnchantData.enchantments()) {
            for (int level : AnvilEnchantData.levelsToRegister(enchantment)) {
                var model = AnvilRecipeFactory.bookRecipe(enchantment, level);
                seen.add(new StackKey(model.addon().stack()));
                if (!model.items().isEmpty()) {
                    recipes.add(new EmiAnvilBookRecipe(recipeId("book", books++), model));
                }
            }
        }
        for (ItemStack book : AnvilEnchantData.observedBooks()) {
            if (seen.add(new StackKey(book))) {
                var model = AnvilRecipeFactory.bookRecipe(book);
                if (!model.items().isEmpty()) {
                    recipes.add(new EmiAnvilBookRecipe(recipeId("book", books++), model));
                }
            }
        }
        int repairs = 0;
        for (var group : repairGroups) {
            AnvilRepairRecipe model = new AnvilRepairRecipe(group.materials(), group.items());
            recipes.add(new EmiAnvilRepairRecipe(recipeId("repair", repairs++), model));
        }
        activeConnection = minecraft.getConnection();
        activeRegistries = access;
        return new Snapshot(List.copyOf(recipes), items, books, repairs,
                new NativeAnvilRepairReplacement(repairs), activeConnection, access);
    }

    static boolean canQuery() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.isSameThread() && activeRegistries != null && activeConnection != null
                && activeConnection == minecraft.getConnection() && activeRegistries == currentRegistries()
                && EmiApi.getRecipeManager().getCategories().contains(ITEM_ENCHANT);
    }

    private static RegistryAccess currentRegistries() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() == null ? null : minecraft.getConnection().registryAccess();
    }

    private static void registerListeners() {
        if (listenersRegistered) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(AnvilEnchantEmiPlugin::onLogout);
        NeoForge.EVENT_BUS.addListener(AnvilEnchantEmiPlugin::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(AnvilEnchantEmiPlugin::onRender);
        NeoForge.EVENT_BUS.addListener(AnvilEnchantEmiPlugin::onScroll);
        NeoForge.EVENT_BUS.addListener(AnvilEnchantEmiPlugin::onDrag);
        NeoForge.EVENT_BUS.addListener(AnvilEnchantEmiPlugin::onRelease);
        listenersRegistered = true;
    }

    private static boolean isRecipeScreen(ScreenEvent event) {
        return event.getScreen().getClass().getName().equals("dev.emi.emi.screen.RecipeScreen");
    }

    private static void onRender(ScreenEvent.Render.Pre event) {
        if (isRecipeScreen(event)) {
            EmiAnvilGridWidget.beginFrame(event.getMouseX(), event.getMouseY());
        } else {
            EmiAnvilGridWidget.clearInteraction();
        }
    }

    private static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (isRecipeScreen(event) && EmiAnvilGridWidget.scrollHovered(
                event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    private static void onDrag(ScreenEvent.MouseDragged.Pre event) {
        if (isRecipeScreen(event) && event.getMouseButton() == 0
                && EmiAnvilGridWidget.dragScrollbar(event.getMouseY())) {
            event.setCanceled(true);
        }
    }

    private static void onRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        EmiAnvilGridWidget.releaseScrollbar();
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        invalidateQuerySnapshot();
        AnvilBrowserData.reset();
        AnvilRecipeFactory.invalidateCache();
    }

    private static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            invalidateQuerySnapshot();
            AnvilBrowserData.reset();
        }
    }

    private static void invalidateQuerySnapshot() {
        activeConnection = null;
        activeRegistries = null;
        EmiViewEpoch.invalidate();
        FocusedEmiRecipes.clear();
        EmiAnvilGridWidget.clearInteraction();
    }

    private record StackKey(Item item, DataComponentPatch components) {
        StackKey(ItemStack stack) {
            this(stack.getItem(), stack.getComponentsPatch());
        }
    }

    private record Snapshot(List<EmiRecipe> recipes, int items, int books, int repairs,
                            NativeAnvilRepairReplacement repairReplacement,
                            Object connection, RegistryAccess registries) {
        static final Snapshot EMPTY = new Snapshot(List.of(), 0, 0, 0,
                new NativeAnvilRepairReplacement(0), null, null);
    }
}
