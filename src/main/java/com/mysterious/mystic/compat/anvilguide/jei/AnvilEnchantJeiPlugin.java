package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mysterious.mystic.compat.anvilguide.AnvilGuide;
import com.mysterious.mystic.compat.anvilguide.data.AnvilBrowserData;
import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.data.IronsSpellsCompat;
import com.mysterious.mystic.compat.anvilguide.data.TaczCompat;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilBookItemsRecipe;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilItemEnchantRecipe;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRecipeFactory;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRepairRecipe;
import com.mojang.logging.LogUtils;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import org.slf4j.Logger;

/**
 * 客户端 JEI 集成。所有动态附魔数据只属于当前连接，退服时一并丢弃。
 * 公共模组入口不引用本类，未安装 JEI 时不会加载可选 API 或客户端类。
 */
@JeiPlugin
public final class AnvilEnchantJeiPlugin implements IModPlugin {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final RecipeType<AnvilItemEnchantRecipe> ITEM_ENCHANT =
            RecipeType.create(AnvilGuide.MOD_ID, "anvil_guide/item_enchant", AnvilItemEnchantRecipe.class);
    public static final RecipeType<AnvilBookItemsRecipe> BOOK_ITEMS =
            RecipeType.create(AnvilGuide.MOD_ID, "anvil_guide/book_items", AnvilBookItemsRecipe.class);
    public static final RecipeType<AnvilRepairRecipe> ANVIL_REPAIR =
            RecipeType.create(AnvilGuide.MOD_ID, "anvil_guide/anvil_repair", AnvilRepairRecipe.class);
    public static final RecipeType<ArcaneImbueRecipe> ARCANE_IMBUE =
            RecipeType.create(AnvilGuide.MOD_ID, "anvil_guide/arcane_imbue", ArcaneImbueRecipe.class);
    public static final RecipeType<TaczQueryRecipe> TACZ_AMMO_GUNS =
            RecipeType.create(AnvilGuide.MOD_ID, "anvil_guide/tacz_ammo_guns", TaczQueryRecipe.class);
    public static final RecipeType<TaczQueryRecipe> TACZ_GUN_ATTACHMENTS =
            RecipeType.create(AnvilGuide.MOD_ID, "anvil_guide/tacz_gun_attachments", TaczQueryRecipe.class);
    public static final RecipeType<TaczQueryRecipe> TACZ_ATTACHMENT_GUNS =
            RecipeType.create(AnvilGuide.MOD_ID, "anvil_guide/tacz_attachment_guns", TaczQueryRecipe.class);

    private static IJeiRuntime runtime;
    private static boolean listenersRegistered;
    private static boolean arcaneCategoryRegistered;
    private static boolean taczCategoriesRegistered;
    private static int refreshDelay = -1;
    private static RecipeSnapshot active = RecipeSnapshot.EMPTY;
    private static final NativeJeiAnvilReplacement nativeAnvilReplacement = new NativeJeiAnvilReplacement();
    private static final Map<RecipeType<Object>, List<Object>> hiddenArcaneRecipes = new LinkedHashMap<>();
    private static final Set<RecipeType<?>> hiddenTaczCategories = new HashSet<>();

    @Override
    public ResourceLocation getPluginUid() {
        return AnvilGuide.id("plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new AnvilItemEnchantCategory(gui),
                new AnvilBookItemsCategory(gui), new AnvilRepairCategory(gui));
        arcaneCategoryRegistered = IronsSpellsCompat.isAvailable();
        if (arcaneCategoryRegistered) {
            registration.addRecipeCategories(new ArcaneImbueCategory(gui, ArcaneImbueCategory.defaultIcon()));
        }
        taczCategoriesRegistered = TaczCompat.isAvailable();
        if (taczCategoriesRegistered) {
            registration.addRecipeCategories(
                    new TaczQueryCategory(gui, TACZ_AMMO_GUNS,
                            TaczQueryRecipe.Kind.AMMO_GUNS, TaczCompat::firstAmmoStack),
                    new TaczQueryCategory(gui, TACZ_GUN_ATTACHMENTS,
                            TaczQueryRecipe.Kind.GUN_ATTACHMENTS, TaczCompat::firstGunStack),
                    new TaczQueryCategory(gui, TACZ_ATTACHMENT_GUNS,
                            TaczQueryRecipe.Kind.ATTACHMENT_GUNS, TaczCompat::firstAttachmentStack));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        active = buildSnapshot(registration.getIngredientManager());
        registration.addRecipes(ITEM_ENCHANT, active.items());
        registration.addRecipes(BOOK_ITEMS, active.books());
        registration.addRecipes(ANVIL_REPAIR, active.repairs());
        if (arcaneCategoryRegistered) {
            registration.addRecipes(ARCANE_IMBUE, active.arcane());
        }
        if (taczCategoriesRegistered) {
            registration.addRecipes(TACZ_AMMO_GUNS, active.ammo());
            registration.addRecipes(TACZ_GUN_ATTACHMENTS, active.guns());
            registration.addRecipes(TACZ_ATTACHMENT_GUNS, active.attachments());
        }
    }

    private static RecipeSnapshot buildSnapshot(IIngredientManager ingredients) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            return minecraft.submit(() -> buildSnapshot(ingredients)).join();
        }
        RegistryAccess access = currentRegistries();
        if (access == null || access.registry(Registries.ENCHANTMENT).isEmpty()) {
            // JEI 可以早于世界数据就绪。保留原页，等连接与标签同步后再补上聚合页。
            return RecipeSnapshot.EMPTY;
        }
        long start = System.nanoTime();
        List<ItemStack> samples = List.copyOf(ingredients.getAllIngredients(VanillaTypes.ITEM_STACK));
        AnvilBrowserData.build("jei", access, samples);
        AnvilRecipeFactory.invalidateCache();
        IronsSpellsCompat.invalidate();
        TaczCompat.invalidate();

        List<AnvilItemEnchantRecipe> items = new ArrayList<>();
        for (ItemStack stack : AnvilEnchantData.enchantableStacks()) {
            items.add(AnvilRecipeFactory.itemRecipe(stack, false));
        }

        List<AnvilBookItemsRecipe> books = new ArrayList<>();
        Set<StackKey> seenBooks = new HashSet<>();
        for (var enchantment : AnvilEnchantData.enchantments()) {
            for (int level : AnvilEnchantData.levelsToRegister(enchantment)) {
                var recipe = AnvilRecipeFactory.bookRecipe(enchantment, level);
                books.add(recipe);
                seenBooks.add(new StackKey(recipe.addon().stack()));
            }
        }
        for (ItemStack stack : AnvilEnchantData.observedBooks()) {
            if (seenBooks.add(new StackKey(stack))) {
                books.add(AnvilRecipeFactory.bookRecipe(stack));
            }
        }

        List<ArcaneImbueRecipe> arcane = new ArrayList<>();
        if (arcaneCategoryRegistered) {
            var scrolls = IronsSpellsCompat.maxLevelScrolls();
            var targets = IronsSpellsCompat.imbueableItems();
            if (!scrolls.isEmpty() && !targets.isEmpty()) {
                var all = IronsSpellsCompat.allScrolls();
                if (!all.isEmpty()) {
                    arcane.add(ArcaneImbueRecipe.merged(all, targets));
                }
                for (ItemStack target : targets) {
                    arcane.add(ArcaneImbueRecipe.forItem(target, scrolls));
                }
            }
        }

        RecipeSnapshot snapshot = new RecipeSnapshot(List.copyOf(items), List.copyOf(books),
                AnvilRepairCategory.recipes(), List.copyOf(arcane),
                taczCategoriesRegistered ? TaczQueryRecipe.ammoPages() : List.of(),
                taczCategoriesRegistered ? TaczQueryRecipe.gunPages() : List.of(),
                taczCategoriesRegistered ? TaczQueryRecipe.attachmentPages() : List.of());
        LOGGER.info("铁砧聚合视图：物品 {}、书 {}、修复材料组 {}，建表耗时 {} ms；数据 {}",
                snapshot.items().size(), snapshot.books().size(), snapshot.repairs().size(),
                (System.nanoTime() - start) / 1_000_000L, AnvilEnchantData.statistics());
        return snapshot;
    }

    private static RegistryAccess currentRegistries() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            return minecraft.getConnection().registryAccess();
        }
        return minecraft.level == null ? null : minecraft.level.registryAccess();
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addRecipeManagerPlugin(new FocusedAnvilRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(ITEM_ENCHANT, Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL);
        registration.addRecipeCatalysts(BOOK_ITEMS, Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL);
        registration.addRecipeCatalysts(ANVIL_REPAIR, Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL);
        if (arcaneCategoryRegistered) {
            ItemStack anvil = IronsSpellsCompat.arcaneAnvilStack();
            if (!anvil.isEmpty()) {
                registration.addRecipeCatalysts(ARCANE_IMBUE, anvil);
            }
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        if (!listenersRegistered) {
            NeoForge.EVENT_BUS.addListener(AnvilEnchantJeiPlugin::onTagsUpdated);
            NeoForge.EVENT_BUS.addListener(AnvilEnchantJeiPlugin::onClientTick);
            NeoForge.EVENT_BUS.addListener(AnvilEnchantJeiPlugin::onLogout);
            listenersRegistered = true;
        }
        updateHiddenCategories();
        if (active == RecipeSnapshot.EMPTY) {
            requestRefresh();
        }
    }

    @Override
    public void onRuntimeRefreshed() {
        requestRefresh();
    }

    private static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            AnvilBrowserData.reset();
            requestRefresh();
        }
    }

    private static void requestRefresh() {
        // 合并标签包与资源重载的相邻通知，在客户端 tick 中统一重建。
        refreshDelay = 2;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (runtime == null || refreshDelay < 0) {
            return;
        }
        if (refreshDelay-- > 0) {
            return;
        }
        if (currentRegistries() == null) {
            refreshDelay = 2;
            return;
        }
        try {
            RecipeSnapshot next = buildSnapshot(runtime.getIngredientManager());
            IRecipeManager manager = runtime.getRecipeManager();
            // JEI 公共 API 用隐藏旧对象、添加新对象刷新查询索引，不修改其他模组的配方。
            manager.hideRecipes(ITEM_ENCHANT, active.items());
            manager.hideRecipes(BOOK_ITEMS, active.books());
            manager.hideRecipes(ANVIL_REPAIR, active.repairs());
            manager.addRecipes(ITEM_ENCHANT, next.items());
            manager.addRecipes(BOOK_ITEMS, next.books());
            manager.addRecipes(ANVIL_REPAIR, next.repairs());
            if (arcaneCategoryRegistered) {
                manager.hideRecipes(ARCANE_IMBUE, active.arcane());
                manager.addRecipes(ARCANE_IMBUE, next.arcane());
            }
            if (taczCategoriesRegistered) {
                manager.hideRecipes(TACZ_AMMO_GUNS, active.ammo());
                manager.hideRecipes(TACZ_GUN_ATTACHMENTS, active.guns());
                manager.hideRecipes(TACZ_ATTACHMENT_GUNS, active.attachments());
                manager.addRecipes(TACZ_AMMO_GUNS, next.ammo());
                manager.addRecipes(TACZ_GUN_ATTACHMENTS, next.guns());
                manager.addRecipes(TACZ_ATTACHMENT_GUNS, next.attachments());
            }
            active = next;
            updateHiddenCategories();
        } catch (RuntimeException | LinkageError exception) {
            LOGGER.warn("刷新铁砧查询失败，恢复原始查询页面", exception);
            restoreVanillaAnvil();
            restoreOptionalPages();
        }
    }

    private static void updateHiddenCategories() {
        if (runtime == null) {
            return;
        }
        IRecipeManager manager = runtime.getRecipeManager();
        try {
            if (!active.items().isEmpty() && !active.books().isEmpty() && !active.repairs().isEmpty()
                    && manager.createRecipeLookup(ITEM_ENCHANT).get().findAny().isPresent()
                    && manager.createRecipeLookup(BOOK_ITEMS).get().findAny().isPresent()
                    && manager.createRecipeLookup(ANVIL_REPAIR).get().findAny().isPresent()) {
                updateNativeAnvilRecipes(manager, true);
            } else {
                updateNativeAnvilRecipes(manager, false);
            }
            if (arcaneCategoryRegistered && !active.arcane().isEmpty()
                    && manager.createRecipeLookup(ARCANE_IMBUE).get().findAny().isPresent()) {
                manager.getRecipeType(IronsSpellsCompat.arcaneAnvilCategoryId()).ifPresent(type -> {
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    RecipeType<Object> rawType = (RecipeType) type;
                    var recipes = manager.createRecipeLookup(rawType).get()
                            .filter(IronsSpellsCompat::isImbueRecipe).toList();
                    if (!recipes.isEmpty()) {
                        manager.hideRecipes(rawType, recipes);
                        hiddenArcaneRecipes.computeIfAbsent(rawType, ignored -> new ArrayList<>()).addAll(recipes);
                    }
                });
            } else {
                restoreArcaneRecipes();
            }
            if (taczCategoriesRegistered && !active.ammo().isEmpty()
                    && !active.guns().isEmpty() && !active.attachments().isEmpty()
                    && manager.createRecipeLookup(TACZ_AMMO_GUNS).get().findAny().isPresent()
                    && manager.createRecipeLookup(TACZ_GUN_ATTACHMENTS).get().findAny().isPresent()
                    && manager.createRecipeLookup(TACZ_ATTACHMENT_GUNS).get().findAny().isPresent()) {
                for (ResourceLocation id : TaczCompat.queryCategoryIds()) {
                    manager.getRecipeType(id).ifPresent(type -> {
                        if (manager.createRecipeLookup(type).get().findAny().isPresent()) {
                            manager.hideRecipeCategory(type);
                            hiddenTaczCategories.add(type);
                        }
                    });
                }
            } else {
                restoreTaczCategories();
            }
        } catch (RuntimeException | LinkageError exception) {
            LOGGER.warn("检查 JEI 替代页时发生错误，恢复原生铁砧配方", exception);
            restoreNativeAnvilRecipes(manager);
        }
    }

    private static void updateNativeAnvilRecipes(IRecipeManager manager, boolean replacementAvailable) {
        nativeAnvilReplacement.update(manager, replacementAvailable);
    }

    private static void restoreNativeAnvilRecipes(IRecipeManager manager) {
        try {
            nativeAnvilReplacement.restore(manager);
        } catch (RuntimeException | LinkageError exception) {
            LOGGER.warn("恢复 JEI 原生铁砧配方失败", exception);
        }
    }

    private static void restoreVanillaAnvil() {
        if (runtime != null) {
            restoreNativeAnvilRecipes(runtime.getRecipeManager());
        }
    }

    private static void restoreOptionalPages() {
        restoreArcaneRecipes();
        restoreTaczCategories();
    }

    private static void restoreArcaneRecipes() {
        if (runtime != null) {
            hiddenArcaneRecipes.forEach(runtime.getRecipeManager()::unhideRecipes);
        }
        hiddenArcaneRecipes.clear();
    }

    private static void restoreTaczCategories() {
        if (runtime != null) {
            hiddenTaczCategories.forEach(runtime.getRecipeManager()::unhideRecipeCategory);
        }
        hiddenTaczCategories.clear();
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        AnvilBrowserData.reset();
        clearRuntime();
    }

    @Override
    public void onRuntimeUnavailable() {
        clearRuntime();
    }

    private static void clearRuntime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.submit(AnvilEnchantJeiPlugin::clearRuntime).join();
            return;
        }
        restoreVanillaAnvil();
        restoreOptionalPages();
        runtime = null;
        refreshDelay = -1;
        active = RecipeSnapshot.EMPTY;
        hiddenArcaneRecipes.clear();
        hiddenTaczCategories.clear();
        resetData();
    }

    private static void resetData() {
        AnvilBrowserData.remove("jei");
        AnvilRecipeFactory.invalidateCache();
        IronsSpellsCompat.invalidate();
        TaczCompat.invalidate();
    }

    public static IJeiRuntime runtime() {
        return runtime;
    }

    private record StackKey(Item item, DataComponentPatch components) {
        StackKey(ItemStack stack) {
            this(stack.getItem(), stack.getComponentsPatch());
        }
    }

    private record RecipeSnapshot(List<AnvilItemEnchantRecipe> items, List<AnvilBookItemsRecipe> books,
                                  List<AnvilRepairRecipe> repairs, List<ArcaneImbueRecipe> arcane,
                                  List<TaczQueryRecipe> ammo, List<TaczQueryRecipe> guns,
                                  List<TaczQueryRecipe> attachments) {
        private static final RecipeSnapshot EMPTY = new RecipeSnapshot(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
