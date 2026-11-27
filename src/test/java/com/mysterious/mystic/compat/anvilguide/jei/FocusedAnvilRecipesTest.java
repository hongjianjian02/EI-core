package com.mysterious.mystic.compat.anvilguide.jei;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilBookItemsRecipe;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilItemEnchantRecipe;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRecipeFactory;
import com.mojang.serialization.Lifecycle;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real Minecraft data with API lookup substitutes; this does not launch or test the JEI GUI. */
class FocusedAnvilRecipesTest {

    private static final List<RecipeIngredientRole> LOOKUP_ROLES =
            List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.OUTPUT);

    private FocusedAnvilRecipes plugin;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void clearPreviousConnection() {
        AnvilEnchantData.reset();
        AnvilRecipeFactory.invalidateCache();
        plugin = new FocusedAnvilRecipes();
    }

    @AfterEach
    void releaseConnectionData() {
        AnvilEnchantData.reset();
        AnvilRecipeFactory.invalidateCache();
    }

    @Test
    void recipeAndUsageKeysReturnOnePageForTheFocusedEquipmentOrBook() {
        Fixture fixture = new Fixture();
        Holder<Enchantment> enchantment = fixture.add("ordinary", 3, Items.DIAMOND_SWORD);
        AnvilEnchantData.build(fixture.access(), List.of());
        ItemStack equipment = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack book = book(enchantment, 3);

        for (RecipeIngredientRole role : LOOKUP_ROLES) {
            IFocus<ItemStack> itemFocus = new ItemFocus(role, equipment);
            assertEquals(List.of(AnvilEnchantJeiPlugin.ITEM_ENCHANT), plugin.getRecipeTypes(itemFocus));
            List<AnvilItemEnchantRecipe> itemPages = plugin.getRecipes(category(AnvilEnchantJeiPlugin.ITEM_ENCHANT), itemFocus);
            assertEquals(1, itemPages.size());
            assertTrue(ItemStack.isSameItemSameComponents(equipment, itemPages.getFirst().input()));
            assertTrue(plugin.getRecipes(category(AnvilEnchantJeiPlugin.BOOK_ITEMS), itemFocus).isEmpty());

            IFocus<ItemStack> bookFocus = new ItemFocus(role, book);
            assertEquals(List.of(AnvilEnchantJeiPlugin.BOOK_ITEMS), plugin.getRecipeTypes(bookFocus));
            List<AnvilBookItemsRecipe> bookPages = plugin.getRecipes(category(AnvilEnchantJeiPlugin.BOOK_ITEMS), bookFocus);
            assertEquals(1, bookPages.size());
            assertTrue(ItemStack.isSameItemSameComponents(book, bookPages.getFirst().addon().stack()));
            assertTrue(plugin.getRecipes(category(AnvilEnchantJeiPlugin.ITEM_ENCHANT), bookFocus).isEmpty());
        }
    }

    @Test
    void equipmentCreatedAfterInitializationKeepsItsDamageAndName() {
        Fixture fixture = new Fixture();
        fixture.add("equipment", 3, Items.DIAMOND_SWORD);
        AnvilEnchantData.build(fixture.access(), List.of());
        int generation = AnvilEnchantData.generation();
        ItemStack damaged = new ItemStack(Items.DIAMOND_SWORD);
        damaged.setDamageValue(37);
        damaged.set(DataComponents.CUSTOM_NAME, Component.literal("Inventory component variant"));

        for (RecipeIngredientRole role : LOOKUP_ROLES) {
            List<AnvilItemEnchantRecipe> pages = plugin.getRecipes(category(AnvilEnchantJeiPlugin.ITEM_ENCHANT),
                    new ItemFocus(role, damaged));
            assertEquals(1, pages.size());
            assertTrue(ItemStack.isSameItemSameComponents(damaged, pages.getFirst().input()));
            assertEquals(37, pages.getFirst().input().getDamageValue());
        }
        assertEquals(generation, AnvilEnchantData.generation(), "R/U must reuse the current index");
    }

    @Test
    void aRealMultiEnchantmentLevel255BookRemainsOneExactPage() {
        Fixture fixture = new Fixture();
        Holder<Enchantment> sword = fixture.add("multi_sword", 3, Items.DIAMOND_SWORD);
        Holder<Enchantment> axe = fixture.add("multi_axe", 2, Items.IRON_AXE);
        AnvilEnchantData.build(fixture.access(), List.of());
        ItemStack book = book(sword, 255);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(book.get(DataComponents.STORED_ENCHANTMENTS));
        stored.set(axe, 2);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable().withTooltip(false));
        book.set(DataComponents.CUSTOM_NAME, Component.literal("Named multi-enchantment book"));

        for (RecipeIngredientRole role : LOOKUP_ROLES) {
            IFocus<ItemStack> focus = new ItemFocus(role, book);
            assertEquals(List.of(AnvilEnchantJeiPlugin.BOOK_ITEMS), plugin.getRecipeTypes(focus));
            List<AnvilBookItemsRecipe> pages = plugin.getRecipes(category(AnvilEnchantJeiPlugin.BOOK_ITEMS), focus);
            assertEquals(1, pages.size());
            AnvilBookItemsRecipe page = pages.getFirst();
            assertTrue(ItemStack.isSameItemSameComponents(book, page.addon().stack()));
            assertEquals(2, page.books().size());
            assertTrue(page.books().stream().anyMatch(entry -> entry.enchantment() == sword && entry.level() == 255));
            assertTrue(page.books().stream().anyMatch(entry -> entry.enchantment() == axe && entry.level() == 2));
            assertEquals(2, page.items().size());
        }
        assertEquals(255, book.get(DataComponents.STORED_ENCHANTMENTS).getLevel(sword));
    }

    @Test
    void storedOnlyPaperAndNonQueryRolesDoNotOfferBookPages() {
        Fixture fixture = new Fixture();
        Holder<Enchantment> enchantment = fixture.add("paper", 3, Items.DIAMOND_SWORD);
        ItemStack paper = new ItemStack(Items.PAPER);
        paper.set(DataComponents.STORED_ENCHANTMENTS, book(enchantment, 3).get(DataComponents.STORED_ENCHANTMENTS));
        AnvilEnchantData.build(fixture.access(), List.of(paper));

        for (RecipeIngredientRole role : LOOKUP_ROLES) {
            IFocus<ItemStack> focus = new ItemFocus(role, paper);
            assertTrue(plugin.getRecipeTypes(focus).isEmpty());
            assertTrue(plugin.getRecipes(category(AnvilEnchantJeiPlugin.BOOK_ITEMS), focus).isEmpty());
            assertTrue(plugin.getRecipes(category(AnvilEnchantJeiPlugin.ITEM_ENCHANT), focus).isEmpty());
        }
        assertTrue(plugin.getRecipeTypes(new ItemFocus(RecipeIngredientRole.CATALYST,
                new ItemStack(Items.DIAMOND_SWORD))).isEmpty());
        assertTrue(plugin.getRecipeTypes(new ItemFocus(RecipeIngredientRole.RENDER_ONLY,
                book(enchantment, 3))).isEmpty());
    }

    @Test
    void anEmptyOrUnavailableRegistryDoesNotGeneratePages() {
        for (RecipeIngredientRole role : LOOKUP_ROLES) {
            IFocus<ItemStack> focus = new ItemFocus(role, new ItemStack(Items.DIAMOND_SWORD));
            assertTrue(plugin.getRecipeTypes(focus).isEmpty());
            assertTrue(plugin.getRecipes(category(AnvilEnchantJeiPlugin.ITEM_ENCHANT), focus).isEmpty());
            assertTrue(plugin.getRecipeTypes(new ItemFocus(role, ItemStack.EMPTY)).isEmpty());
        }
        assertTrue(plugin.getRecipes(category(AnvilEnchantJeiPlugin.ITEM_ENCHANT)).isEmpty());
        assertTrue(plugin.getRecipes(category(AnvilEnchantJeiPlugin.BOOK_ITEMS)).isEmpty());
    }

    @Test
    void anOldBookWithARemovedIdCannotThrowAndDisableTheLookupPlugin() {
        Fixture first = new Fixture();
        Holder<Enchantment> removed = first.add("removed", 3, Items.DIAMOND_SWORD);
        AnvilEnchantData.build(first.access(), List.of());
        ItemStack oldBook = book(removed, 2);
        Fixture second = new Fixture();
        Holder<Enchantment> current = second.add("current", 1, Items.IRON_AXE);
        AnvilEnchantData.build(second.access(), List.of());

        for (RecipeIngredientRole role : LOOKUP_ROLES) {
            IFocus<ItemStack> oldFocus = new ItemFocus(role, oldBook);
            assertTrue(assertDoesNotThrow(() -> plugin.getRecipeTypes(oldFocus)).isEmpty());
            assertTrue(assertDoesNotThrow(() -> plugin.getRecipes(category(AnvilEnchantJeiPlugin.BOOK_ITEMS), oldFocus)).isEmpty());
            List<AnvilBookItemsRecipe> currentPages = plugin.getRecipes(category(AnvilEnchantJeiPlugin.BOOK_ITEMS),
                    new ItemFocus(role, book(current, 1)));
            assertEquals(1, currentPages.size(), "a rejected old stack must not break subsequent lookups");
            assertSame(current, currentPages.getFirst().books().getFirst().enchantment());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> IRecipeCategory<T> category(RecipeType<T> type) {
        return (IRecipeCategory<T>) Proxy.newProxyInstance(IRecipeCategory.class.getClassLoader(),
                new Class<?>[] { IRecipeCategory.class }, (proxy, method, args) -> {
                    if (method.getName().equals("getRecipeType")) {
                        return type;
                    }
                    throw new UnsupportedOperationException("Lookup should not initialize a GUI: " + method.getName());
                });
    }

    private static ItemStack book(Holder<Enchantment> enchantment, int level) {
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(enchantment, level);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return book;
    }

    private record ItemFocus(RecipeIngredientRole role, ItemStack stack) implements IFocus<ItemStack> {
        @Override
        public ITypedIngredient<ItemStack> getTypedValue() {
            return new TypedStack(stack);
        }

        @Override
        public RecipeIngredientRole getRole() {
            return role;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<IFocus<T>> checkedCast(IIngredientType<T> type) {
            return type.equals(VanillaTypes.ITEM_STACK) ? Optional.of((IFocus<T>) this) : Optional.empty();
        }
    }

    private record TypedStack(ItemStack stack) implements ITypedIngredient<ItemStack> {
        @Override
        public IIngredientType<ItemStack> getType() {
            return VanillaTypes.ITEM_STACK;
        }

        @Override
        public ItemStack getIngredient() {
            return stack;
        }

        @Override
        public ITypedIngredient<ItemStack> normalize(IIngredientHelper<ItemStack> helper) {
            return new TypedStack(helper.normalizeIngredient(stack));
        }
    }

    private static final class Fixture {
        private final MappedRegistry<Enchantment> registry =
                new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable());

        Holder<Enchantment> add(String name, int maxLevel, Item supported) {
            Enchantment.EnchantmentDefinition definition = new Enchantment.EnchantmentDefinition(
                    HolderSet.direct(supported.builtInRegistryHolder()), Optional.empty(), 10, maxLevel,
                    new Enchantment.Cost(1, 0), new Enchantment.Cost(10, 0), 4,
                    List.of(EquipmentSlotGroup.ANY));
            Enchantment enchantment = new Enchantment(Component.literal(name), definition,
                    HolderSet.direct(), DataComponentMap.EMPTY);
            return registry.register(ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath("focus_test", name)), enchantment, RegistrationInfo.BUILT_IN);
        }

        RegistryAccess access() {
            return new RegistryAccess.ImmutableRegistryAccess(List.of(registry.freeze()));
        }
    }
}
