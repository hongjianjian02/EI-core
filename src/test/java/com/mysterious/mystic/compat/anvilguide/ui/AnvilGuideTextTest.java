package com.mysterious.mystic.compat.anvilguide.ui;

import java.util.List;
import java.util.Optional;

import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.data.BookEntry;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilBookItemsRecipe;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRecipeFactory;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Shared tooltip rules use actual anvil components and registries without loading either browser. */
class AnvilGuideTextTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void clearPreviousConnection() {
        AnvilEnchantData.reset();
        AnvilRecipeFactory.invalidateCache();
    }

    @AfterEach
    void releaseConnection() {
        AnvilEnchantData.reset();
        AnvilRecipeFactory.invalidateCache();
    }

    @Test
    void aMultiEnchantmentBookOnlyDescribesEntriesApplicableToTheHoveredTarget() {
        Fixture fixture = new Fixture();
        Holder<Enchantment> sword = fixture.add("sword", "Sword enchantment", Items.DIAMOND_SWORD,
                3, 4, HolderSet.direct());
        Holder<Enchantment> axe = fixture.add("axe", "Axe enchantment", Items.IRON_AXE,
                2, 4, HolderSet.direct(sword));
        AnvilEnchantData.build(fixture.access(), List.of());
        ItemStack source = book(sword, axe);
        AnvilBookItemsRecipe recipe = AnvilRecipeFactory.bookRecipe(source);

        ItemStack swordTarget = new ItemStack(Items.DIAMOND_SWORD);
        AnvilGuideText.ItemDetails swordDetails = AnvilGuideText.itemDetails(recipe.books(), source, swordTarget);
        assertEquals("Sword enchantment", argument(swordDetails.tooltip(), "book_applicable"));
        assertTrue(hasLine(swordDetails.tooltip(), "book_partial"));
        assertFalse(swordDetails.conflicts(), "an incompatible entry that cannot apply must not add a conflict badge");
        assertFalse(AnvilGuideText.hasItemConflicts(recipe.books(), source, swordTarget));
        assertTrue(hasLine(swordDetails.tooltip(), "no_mutex"));

        ItemStack axeTarget = new ItemStack(Items.IRON_AXE);
        AnvilGuideText.ItemDetails axeDetails = AnvilGuideText.itemDetails(recipe.books(), source, axeTarget);
        assertEquals("Axe enchantment", argument(axeDetails.tooltip(), "book_applicable"));
        assertFalse(axeDetails.conflicts());
        assertFalse(AnvilGuideText.hasItemConflicts(recipe.books(), source, axeTarget));
    }

    @Test
    void multipleApplicableEntriesShareOneConflictInBothTheTooltipAndBadge() {
        Fixture fixture = new Fixture();
        Holder<Enchantment> first = fixture.add("first", "First", Items.DIAMOND_SWORD,
                1, 4, HolderSet.direct());
        Holder<Enchantment> second = fixture.add("second", "Second", Items.DIAMOND_SWORD,
                1, 4, HolderSet.direct());
        fixture.add("shared_conflict", "Shared conflict", Items.DIAMOND_SWORD,
                1, 4, HolderSet.direct(first, second));
        AnvilEnchantData.build(fixture.access(), List.of());
        ItemStack source = book(first, second);
        List<BookEntry> entries = List.of(AnvilEnchantData.bookEntry(first, 1),
                AnvilEnchantData.bookEntry(second, 1));
        ItemStack target = new ItemStack(Items.DIAMOND_SWORD);

        AnvilGuideText.ItemDetails details = AnvilGuideText.itemDetails(entries, source, target);
        assertTrue(details.conflicts());
        assertTrue(AnvilGuideText.hasItemConflicts(entries, source, target));
        assertEquals("Shared conflict", argument(details.tooltip(), "mutex"),
                "the same conflicting enchantment must appear once even when both book entries conflict with it");
        assertFalse(hasLine(details.tooltip(), "no_mutex"));

        ItemStack unsupported = new ItemStack(Items.IRON_AXE);
        assertFalse(AnvilGuideText.itemDetails(entries, source, unsupported).conflicts());
        assertFalse(AnvilGuideText.hasItemConflicts(entries, source, unsupported));
    }

    @Test
    void anOldWorldBookUsesCurrentHoldersAndRulesAfterTheRegistryIsRebuilt() {
        Fixture first = new Fixture();
        Holder<Enchantment> oldHolder = first.add("changed", "Old description", Items.DIAMOND_SWORD,
                3, 4, HolderSet.direct());
        AnvilEnchantData.build(first.access(), List.of());
        ItemStack oldBook = book(oldHolder);
        ItemEnchantments.Mutable oldLevels = new ItemEnchantments.Mutable(oldBook.get(DataComponents.STORED_ENCHANTMENTS));
        oldLevels.set(oldHolder, 2);
        oldBook.set(DataComponents.STORED_ENCHANTMENTS, oldLevels.toImmutable());

        Fixture next = new Fixture();
        Holder<Enchantment> current = next.add("changed", "Current description", Items.IRON_AXE,
                1, 10, HolderSet.direct());
        next.add("current_conflict", "Current conflict", Items.IRON_AXE,
                1, 4, HolderSet.direct(current));
        AnvilEnchantData.build(next.access(), List.of());
        AnvilBookItemsRecipe recipe = AnvilRecipeFactory.bookRecipe(oldBook);
        BookEntry entry = recipe.books().getFirst();
        assertSame(current, entry.enchantment());
        assertSame(current, recipe.addon().stack().get(DataComponents.STORED_ENCHANTMENTS).keySet().iterator().next());
        assertSame(oldHolder, oldBook.get(DataComponents.STORED_ENCHANTMENTS).keySet().iterator().next(),
                "the retained incoming book must remain owned by its caller");

        ItemStack axe = new ItemStack(Items.IRON_AXE);
        List<Component> tooltip = AnvilGuideText.bookTooltip(axe, entry);
        assertEquals("Current description", argument(tooltip, "enchantment"));
        assertEquals(5, argument(tooltip, "cost"), "book cost must use the new definition's cost and level cap");
        assertEquals("I", argument(tooltip, "observed_level"));
        assertEquals("I", argument(tooltip, "highest_level"));
        assertEquals("Current conflict", argument(tooltip, "conflicts"));
        assertFalse(hasLine(tooltip, "lower_level"));
        assertTrue(AnvilGuideText.itemDetails(recipe.books(), recipe.addon().stack(), axe).conflicts());
        assertFalse(AnvilGuideText.itemDetails(recipe.books(), recipe.addon().stack(),
                new ItemStack(Items.DIAMOND_SWORD)).conflicts(), "the old world's supported target must no longer apply");
    }

    private static boolean hasLine(List<Component> lines, String suffix) {
        return lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("eicore.anvil_guide.tooltip." + suffix));
    }

    private static Object argument(List<Component> lines, String suffix) {
        return lines.stream().map(Component::getContents)
                .filter(TranslatableContents.class::isInstance).map(TranslatableContents.class::cast)
                .filter(contents -> contents.getKey().equals("eicore.anvil_guide.tooltip." + suffix))
                .findFirst().orElseThrow().getArgs()[0];
    }

    @SafeVarargs
    private static ItemStack book(Holder<Enchantment>... enchantments) {
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Holder<Enchantment> enchantment : enchantments) {
            stored.set(enchantment, 1);
        }
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        stack.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return stack;
    }

    private static final class Fixture {
        private final MappedRegistry<Enchantment> registry = new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable());

        Holder<Enchantment> add(String name, String description, Item target, int maxLevel, int anvilCost,
                HolderSet<Enchantment> exclusive) {
            Enchantment.EnchantmentDefinition definition = new Enchantment.EnchantmentDefinition(
                    HolderSet.direct(target.builtInRegistryHolder()), Optional.empty(),
                    10, maxLevel, new Enchantment.Cost(1, 0), new Enchantment.Cost(10, 0),
                    anvilCost, List.of(EquipmentSlotGroup.ANY));
            Enchantment enchantment = new Enchantment(Component.literal(description), definition,
                    exclusive, DataComponentMap.EMPTY);
            return registry.register(ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath("guide_text_test", name)), enchantment, RegistrationInfo.BUILT_IN);
        }

        RegistryAccess access() {
            return new RegistryAccess.ImmutableRegistryAccess(List.of(registry.freeze()));
        }
    }
}
