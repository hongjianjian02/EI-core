package com.mysterious.mystic.compat.anvilguide.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mysterious.mystic.compat.anvilguide.recipe.AnvilBookItemsRecipe;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRecipeFactory;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilEnchantDataTest {

    private Map<TagKey<Item>, List<Holder<Item>>> originalItemTags;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void rememberItemTags() throws ReflectiveOperationException {
        originalItemTags = new LinkedHashMap<>();
        BuiltInRegistries.ITEM.getTags().forEach(pair ->
                originalItemTags.put(pair.getFirst(), pair.getSecond().stream().toList()));
        AnvilEnchantData.reset();
        AnvilRecipeFactory.invalidateCache();
        clearTierIngredientCaches();
    }

    @AfterEach
    void releaseConnectionData() throws ReflectiveOperationException {
        AnvilEnchantData.reset();
        AnvilRecipeFactory.invalidateCache();
        BuiltInRegistries.ITEM.resetTags();
        BuiltInRegistries.ITEM.bindTags(originalItemTags);
        clearTierIngredientCaches();
    }

    @Test
    void anvilUsesSupportedItemsEvenWhenTheItemIsNotPrimary() {
        Fixture fixture = new Fixture();
        Holder<Enchantment> enchantment = fixture.add("supported", 3,
                HolderSet.direct(Items.DIAMOND_SWORD.builtInRegistryHolder(), Items.IRON_SWORD.builtInRegistryHolder()),
                Optional.of(HolderSet.direct(Items.IRON_SWORD.builtInRegistryHolder())), HolderSet.direct());
        ItemStack target = new ItemStack(Items.DIAMOND_SWORD);
        AnvilEnchantData.build(fixture.access(), List.of());

        assertFalse(target.isPrimaryItemFor(enchantment), "fixture must distinguish primary from supported");
        assertTrue(target.supportsEnchantment(enchantment));
        assertTrue(AnvilEnchantData.highestBooks(target).stream().anyMatch(book -> book.enchantment().equals(enchantment)));

        ItemStack withoutStorage = target.copy();
        withoutStorage.remove(DataComponents.ENCHANTMENTS);
        assertFalse(AnvilEnchantData.canApply(enchantment, withoutStorage),
                "AnvilMenu requires an enchantment component before applying ordinary book enchantments");
    }

    @Test
    void asymmetricExclusiveSetsAndCurseTagsUseTheCurrentRegistry() {
        Fixture fixture = new Fixture();
        HolderSet<Item> swords = HolderSet.direct(Items.DIAMOND_SWORD.builtInRegistryHolder());
        Holder<Enchantment> ordinary = fixture.add("ordinary", 1, swords, Optional.empty(), HolderSet.direct());
        Holder<Enchantment> curse = fixture.add("curse", 1, swords, Optional.empty(), HolderSet.direct(ordinary));
        fixture.registry.bindTags(Map.of(EnchantmentTags.CURSE, List.of(curse)));
        AnvilEnchantData.build(fixture.access(), List.of());

        assertFalse(Enchantment.areCompatible(ordinary, curse));
        assertTrue(AnvilEnchantData.conflictsFor(ordinary, new ItemStack(Items.DIAMOND_SWORD)).contains(curse));
        assertTrue(AnvilEnchantData.conflictsFor(curse, new ItemStack(Items.DIAMOND_SWORD)).contains(ordinary));
        assertTrue(EnchantmentCompat.isCurse(curse));
        assertFalse(EnchantmentCompat.isCurse(ordinary));
        List<BookEntry> books = AnvilEnchantData.highestBooks(Items.DIAMOND_SWORD);
        assertSame(ordinary, books.getFirst().enchantment());
        assertSame(curse, books.getLast().enchantment());
    }

    @Test
    void observedLevel255DoesNotInventIntermediateBooksOrSurviveAConnectionReset() {
        Fixture first = new Fixture();
        Holder<Enchantment> oldHolder = first.swordEnchantment("same_id", 3);
        ItemStack highBook = book(oldHolder, 255);
        AnvilEnchantData.build(first.access(), List.of(highBook));

        assertEquals(255, AnvilEnchantData.maxLevel(oldHolder));
        assertEquals(List.of(1, 2, 3, 255), AnvilEnchantData.levelsToRegister(oldHolder));
        assertEquals(255, AnvilEnchantData.highestBooks(Items.DIAMOND_SWORD).getFirst().level());
        assertEquals(6, AnvilEnchantData.anvilCost(oldHolder, 255),
                "ordinary AnvilMenu applies the definition's level cap and half book cost");
        assertEquals(List.of(255, 3, 2, 1), AnvilEnchantData.allBooks(Items.DIAMOND_SWORD).stream().map(BookEntry::level).toList());
        assertTrue(ItemStack.isSameItemSameComponents(highBook, AnvilEnchantData.observedBooks().getFirst()));

        AnvilEnchantData.reset();
        assertTrue(AnvilEnchantData.enchantments().isEmpty());
        assertTrue(AnvilEnchantData.observedBooks().isEmpty());
        Fixture second = new Fixture();
        Holder<Enchantment> newHolder = second.swordEnchantment("same_id", 3);
        AnvilEnchantData.build(second.access(), List.of());
        assertNotSame(oldHolder, newHolder);
        assertEquals(3, AnvilEnchantData.maxLevel(newHolder));
        assertSame(newHolder, AnvilEnchantData.highestBooks(Items.DIAMOND_SWORD).getFirst().enchantment());
        assertEquals(List.of(1, 2, 3), AnvilEnchantData.levelsToRegister(newHolder));
    }

    @Test
    void componentVariantsStaySeparateAndCountsDoNotCreateDuplicateRecipes() {
        Fixture fixture = new Fixture();
        Holder<Enchantment> enchantment = fixture.swordEnchantment("variants", 2);
        ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Component variant"));
        ItemStack duplicate = named.copyWithCount(2);
        AnvilEnchantData.build(fixture.access(), List.of(named, duplicate));

        List<ItemStack> targets = AnvilEnchantData.groupedItems(enchantment).values().stream().flatMap(List::stream).toList();
        assertEquals(2, targets.stream().filter(stack -> stack.is(Items.DIAMOND_SWORD)).count());
        assertTrue(targets.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(named, stack)));
        assertTrue(targets.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(new ItemStack(Items.DIAMOND_SWORD), stack)));
        assertEquals(2, AnvilEnchantData.enchantableStacks().size());

        ItemStack damaged = named.copy();
        damaged.setDamageValue(5);
        assertFalse(AnvilEnchantData.highestBooks(damaged).isEmpty(), "new damage components must remain queryable");
        assertTrue(ItemStack.isSameItemSameComponents(named, duplicate), "Minecraft compares components separately from count");
    }

    @Test
    void repairMaterialsMergeOnlyWhenTheirEntireTargetSetsMatch() throws ReflectiveOperationException {
        Map<TagKey<Item>, List<Holder<Item>>> tags = new LinkedHashMap<>(originalItemTags);
        tags.put(ItemTags.PLANKS, List.of(Items.OAK_PLANKS.builtInRegistryHolder(),
                Items.BIRCH_PLANKS.builtInRegistryHolder(), Items.JUNGLE_PLANKS.builtInRegistryHolder()));
        tags.put(ItemTags.STONE_TOOL_MATERIALS, List.of(Items.OAK_PLANKS.builtInRegistryHolder(),
                Items.COBBLESTONE.builtInRegistryHolder()));
        BuiltInRegistries.ITEM.bindTags(tags);
        clearTierIngredientCaches();
        Fixture fixture = new Fixture();
        AnvilEnchantData.build(fixture.access(), List.of());

        AnvilEnchantData.RepairGroup birch = groupFor(Items.BIRCH_PLANKS);
        AnvilEnchantData.RepairGroup oak = groupFor(Items.OAK_PLANKS);
        AnvilEnchantData.RepairGroup cobblestone = groupFor(Items.COBBLESTONE);
        assertTrue(birch.materials().stream().anyMatch(stack -> stack.is(Items.JUNGLE_PLANKS)),
                "birch and jungle planks repair exactly the same equipment");
        assertFalse(birch.materials().stream().anyMatch(stack -> stack.is(Items.OAK_PLANKS)));
        assertTrue(birch.items().stream().anyMatch(stack -> stack.is(Items.WOODEN_SWORD)));
        assertFalse(birch.items().stream().anyMatch(stack -> stack.is(Items.STONE_SWORD)));
        assertTrue(oak.items().stream().anyMatch(stack -> stack.is(Items.WOODEN_SWORD)));
        assertTrue(oak.items().stream().anyMatch(stack -> stack.is(Items.STONE_SWORD)));
        assertTrue(cobblestone.items().stream().anyMatch(stack -> stack.is(Items.STONE_SWORD)));
        assertFalse(cobblestone.items().stream().anyMatch(stack -> stack.is(Items.WOODEN_SWORD)));
        assertFalse(cobblestone.materials().stream().anyMatch(stack -> stack.is(Items.OAK_PLANKS)),
                "overlapping sets must not be merged transitively through oak planks");
        ItemStack damagedWoodenSword = new ItemStack(Items.WOODEN_SWORD);
        damagedWoodenSword.setDamageValue(5);
        assertTrue(AnvilEnchantData.repairMaterials(damagedWoodenSword).stream()
                .anyMatch(material -> material.is(Items.BIRCH_PLANKS)),
                "damage components outside initialization samples retain their repair-material lookup");
    }

    @Test
    void aNewNamedMultiEnchantmentBookPreservesComponentsAndUsesTheTargetUnion() {
        Fixture fixture = new Fixture();
        Holder<Enchantment> sword = fixture.swordEnchantment("multi_sword", 3);
        Holder<Enchantment> axe = fixture.add("multi_axe", 2,
                HolderSet.direct(Items.IRON_AXE.builtInRegistryHolder()), Optional.empty(), HolderSet.direct());
        AnvilEnchantData.build(fixture.access(), List.of());
        int initialGeneration = AnvilEnchantData.generation();

        ItemStack book = book(sword, 255);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(book.get(DataComponents.STORED_ENCHANTMENTS));
        stored.set(axe, 2);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        book.set(DataComponents.CUSTOM_NAME, Component.literal("A real multi-enchantment book"));
        AnvilBookItemsRecipe recipe = AnvilRecipeFactory.bookRecipe(book);

        assertTrue(ItemStack.isSameItemSameComponents(book, recipe.addon().stack()));
        assertEquals(2, recipe.books().size());
        assertTrue(recipe.books().stream().anyMatch(entry -> entry.enchantment().equals(sword) && entry.level() == 255));
        assertEquals(2, recipe.items().size());
        assertTrue(recipe.items().stream().anyMatch(target -> target.is(Items.DIAMOND_SWORD)));
        assertTrue(recipe.items().stream().anyMatch(target -> target.is(Items.IRON_AXE)));
        assertEquals(initialGeneration, AnvilEnchantData.generation(),
                "a real book lookup should reuse the runtime index rather than rebuild it");
        assertEquals(3, AnvilEnchantData.maxLevel(sword),
                "a query outside the JEI initialization inventory must not mutate observed global levels");
    }

    @Test
    void aBookAndHolderFromAnOlderConnectionUseTheNewDefinitionAndKeepOtherComponents() {
        Fixture first = new Fixture();
        Holder<Enchantment> oldHolder = first.swordEnchantment("changed", 3);
        AnvilEnchantData.build(first.access(), List.of());
        ItemStack oldBook = book(oldHolder, 2);
        oldBook.set(DataComponents.CUSTOM_NAME, Component.literal("Keep this name"));
        oldBook.set(DataComponents.STORED_ENCHANTMENTS,
                oldBook.get(DataComponents.STORED_ENCHANTMENTS).withTooltip(false));
        ItemStack oldEquipment = new ItemStack(Items.DIAMOND_SWORD);
        oldEquipment.enchant(oldHolder, 2);

        Fixture second = new Fixture();
        Holder<Enchantment> newHolder = second.add("changed", 1,
                HolderSet.direct(Items.IRON_AXE.builtInRegistryHolder()), Optional.empty(), HolderSet.direct());
        AnvilEnchantData.build(second.access(), List.of());

        assertTrue(oldHolder.isBound(), "reset does not invalidate foreign Holder objects held by a GUI");
        assertNotSame(oldHolder, newHolder);
        assertEquals(oldHolder, newHolder, "NeoForge Holder equality compares registry keys across connections");
        assertSame(newHolder, AnvilEnchantData.currentHolder(oldHolder).orElseThrow());
        assertSame(newHolder, AnvilEnchantData.bookEntry(oldHolder, 2).enchantment());
        assertEquals(1, AnvilEnchantData.maxLevel(oldHolder));
        assertEquals(2, AnvilEnchantData.anvilCost(oldHolder, 2));
        assertFalse(AnvilEnchantData.canApply(oldHolder, new ItemStack(Items.DIAMOND_SWORD)));
        assertTrue(AnvilEnchantData.canApply(oldHolder, new ItemStack(Items.IRON_AXE)));

        AnvilBookItemsRecipe recipe = AnvilRecipeFactory.bookRecipe(oldBook);
        assertEquals(1, recipe.items().size());
        assertTrue(recipe.items().getFirst().is(Items.IRON_AXE));
        assertEquals(oldBook.get(DataComponents.CUSTOM_NAME), recipe.addon().stack().get(DataComponents.CUSTOM_NAME));
        ItemEnchantments rebound = recipe.addon().stack().get(DataComponents.STORED_ENCHANTMENTS);
        assertEquals(2, rebound.getLevel(newHolder));
        assertEquals(1, rebound.size());
        // contains/getLevel 使用 NeoForge 的按注册键 equals，必须检查实际存储的对象身份。
        assertSame(newHolder, rebound.keySet().iterator().next());
        assertFalse(rebound.keySet().stream().anyMatch(holder -> holder == oldHolder));
        assertEquals(rebound, rebound.withTooltip(false), "hidden tooltip state must survive rebinding");
        assertSame(oldHolder, oldBook.get(DataComponents.STORED_ENCHANTMENTS).keySet().iterator().next(),
                "rebinding must not mutate the incoming item stack");
        assertSame(newHolder, AnvilEnchantData.rebindEnchantments(oldEquipment).get(DataComponents.ENCHANTMENTS)
                .keySet().iterator().next());
        assertSame(newHolder, AnvilRecipeFactory.itemRecipe(oldEquipment, false).input().get(DataComponents.ENCHANTMENTS)
                .keySet().iterator().next(), "an item query must not return its old enchantment components to JEI");

        AnvilEnchantData.build(second.access(), List.of(oldBook));
        assertSame(newHolder, AnvilEnchantData.observedBooks().getFirst().get(DataComponents.STORED_ENCHANTMENTS)
                .keySet().iterator().next(), "initialization samples must also be rebound");
    }

    @Test
    void anEnchantmentMissingFromTheNewRegistryIsRejectedInsteadOfUsingItsOldRules() {
        Fixture first = new Fixture();
        Holder<Enchantment> removed = first.swordEnchantment("removed", 3);
        AnvilEnchantData.build(first.access(), List.of());
        ItemStack oldBook = book(removed, 2);
        Fixture second = new Fixture();
        AnvilEnchantData.build(second.access(), List.of());

        assertTrue(removed.isBound());
        assertTrue(AnvilEnchantData.currentHolder(removed).isEmpty());
        assertFalse(AnvilEnchantData.canApply(removed, new ItemStack(Items.DIAMOND_SWORD)));
        assertTrue(AnvilEnchantData.groupedItems(removed).isEmpty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AnvilEnchantData.bookEntry(removed, 2));
        assertTrue(exception.getMessage().contains("anvil_test:removed"));
        assertThrows(IllegalArgumentException.class, () -> AnvilRecipeFactory.bookRecipe(oldBook));
        AnvilEnchantData.build(second.access(), List.of(oldBook));
        assertTrue(AnvilEnchantData.observedBooks().isEmpty());
    }

    @Test
    void aCustomCarrierUsesTheActualCraftingComponentAndStoredOnlyPaperIsNotAnAnvilBook() {
        Fixture fixture = new Fixture();
        Holder<Enchantment> enchantment = fixture.swordEnchantment("carrier", 3);
        ItemStack storedOnly = new ItemStack(Items.PAPER);
        storedOnly.set(DataComponents.STORED_ENCHANTMENTS, book(enchantment, 3).get(DataComponents.STORED_ENCHANTMENTS));
        AnvilEnchantData.build(fixture.access(), List.of(storedOnly));

        assertFalse(AnvilEnchantData.isAnvilBook(storedOnly));
        assertTrue(AnvilEnchantData.observedBooks().isEmpty());
        assertTrue(AnvilEnchantData.highestBooks(storedOnly).isEmpty());
        assertFalse(AnvilEnchantData.canApply(enchantment, new ItemStack(Items.DIAMOND_SWORD), storedOnly));
        assertThrows(IllegalArgumentException.class, () -> AnvilRecipeFactory.bookRecipe(storedOnly));

        ItemStack carrier = storedOnly.copy();
        carrier.set(DataComponents.ENCHANTMENTS, book(enchantment, 1).get(DataComponents.STORED_ENCHANTMENTS));
        carrier.set(DataComponents.CUSTOM_NAME, Component.literal("Custom carrier"));
        AnvilBookItemsRecipe recipe = AnvilRecipeFactory.bookRecipe(carrier);
        assertTrue(AnvilEnchantData.isAnvilBook(carrier));
        assertEquals(1, recipe.books().getFirst().level(),
                "ordinary AnvilMenu reads ENCHANTMENTS for a custom item, not its stored-only data");
        assertTrue(ItemStack.isSameItemSameComponents(carrier, recipe.addon().stack()));
        assertTrue(recipe.items().stream().anyMatch(target -> target.is(Items.DIAMOND_SWORD)));
    }

    @Test
    void lateNamedMaterialsKeepRepairTargetsIncludingEquipmentComponentVariants() {
        Fixture fixture = new Fixture();
        ItemStack target = new ItemStack(Items.IRON_SWORD);
        target.setDamageValue(21);
        target.set(DataComponents.CUSTOM_NAME, Component.literal("Damaged target variant"));
        AnvilEnchantData.build(fixture.access(), List.of(target, target.copyWithCount(2)));
        int generation = AnvilEnchantData.generation();
        ItemStack material = new ItemStack(Items.IRON_INGOT);
        material.set(DataComponents.CUSTOM_NAME, Component.literal("Late material variant"));

        List<ItemStack> repairs = AnvilEnchantData.repairableItems(material);
        assertEquals(2, repairs.stream().filter(stack -> stack.is(Items.IRON_SWORD)).count());
        assertTrue(repairs.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, target)));
        assertEquals(repairs, AnvilEnchantData.repairableItems(material.copyWithCount(64)));
        assertFalse(AnvilEnchantData.repairableItems(new ItemStack(Items.DIAMOND)).stream()
                .anyMatch(stack -> stack.is(Items.IRON_SWORD)));
        assertEquals(generation, AnvilEnchantData.generation(), "material lookups reuse the collected target index");
        assertEquals("Late material variant", material.getHoverName().getString());
    }

    @Test
    void focusedRepairCacheDoesNotSurviveDisconnectOrRetainOldComponentTargets() {
        Fixture first = new Fixture();
        ItemStack named = new ItemStack(Items.IRON_SWORD);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Old connection target"));
        AnvilEnchantData.build(first.access(), List.of(named));
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        assertTrue(AnvilEnchantData.repairableItems(iron).stream()
                .anyMatch(target -> ItemStack.isSameItemSameComponents(target, named)));
        AnvilEnchantData.reset();
        assertTrue(AnvilEnchantData.repairableItems(iron).isEmpty());
        Fixture next = new Fixture();
        AnvilEnchantData.build(next.access(), List.of());
        assertFalse(AnvilEnchantData.repairableItems(iron).stream()
                .anyMatch(target -> ItemStack.isSameItemSameComponents(target, named)));
    }

    /** Direct fixture tag rebinding bypasses normal game lifecycle and must release shared vanilla ingredient caches. */
    private static void clearTierIngredientCaches() throws ReflectiveOperationException {
        var itemStacks = Ingredient.class.getDeclaredField("itemStacks");
        var stackingIds = Ingredient.class.getDeclaredField("stackingIds");
        itemStacks.setAccessible(true);
        stackingIds.setAccessible(true);
        for (Tiers tier : Tiers.values()) {
            Ingredient ingredient = tier.getRepairIngredient();
            itemStacks.set(ingredient, null);
            stackingIds.set(ingredient, null);
        }
    }

    private static AnvilEnchantData.RepairGroup groupFor(Item material) {
        return AnvilEnchantData.repairGroups().stream()
                .filter(group -> group.materials().stream().anyMatch(stack -> stack.is(material)))
                .findFirst().orElseThrow();
    }

    private static ItemStack book(Holder<Enchantment> enchantment, int level) {
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(enchantment, level);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return book;
    }

    private static final class Fixture {
        private final MappedRegistry<Enchantment> registry = new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable());

        Holder<Enchantment> swordEnchantment(String name, int maxLevel) {
            return add(name, maxLevel, HolderSet.direct(Items.DIAMOND_SWORD.builtInRegistryHolder()),
                    Optional.empty(), HolderSet.direct());
        }

        Holder<Enchantment> add(String name, int maxLevel, HolderSet<Item> supported,
                                Optional<HolderSet<Item>> primary, HolderSet<Enchantment> exclusive) {
            Enchantment.EnchantmentDefinition definition = new Enchantment.EnchantmentDefinition(
                    supported, primary, 10, maxLevel, new Enchantment.Cost(1, 0), new Enchantment.Cost(10, 0),
                    4, List.of(EquipmentSlotGroup.ANY));
            Enchantment enchantment = new Enchantment(Component.literal(name), definition, exclusive, DataComponentMap.EMPTY);
            return registry.register(ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath("anvil_test", name)), enchantment, RegistrationInfo.BUILT_IN);
        }

        RegistryAccess access() {
            return new RegistryAccess.ImmutableRegistryAccess(List.of(registry.freeze()));
        }
    }
}
