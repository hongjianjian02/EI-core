package com.mysterious.mystic.compat.anvilguide.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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

/** Browser source ownership tests use real components and registries without loading either browser. */
class AnvilBrowserDataTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void clearPreviousSources() {
        AnvilBrowserData.reset();
    }

    @AfterEach
    void releaseSources() {
        AnvilBrowserData.reset();
    }

    @Test
    void browserVariantsAreMergedAndReplacingOrRemovingOneSourceRetainsTheOther() {
        Fixture fixture = new Fixture(3);
        ItemStack jeiVariant = namedSword("JEI variant");
        ItemStack emiVariant = namedSword("EMI variant");
        ItemStack highBook = book(fixture.enchantment, 255);
        List<ItemStack> supplied = new ArrayList<>(List.of(jeiVariant));
        AnvilBrowserData.build("jei", fixture.access(), supplied);

        // A browser may mutate its ingredient objects after registration; source ownership must survive.
        supplied.clear();
        jeiVariant.set(DataComponents.CUSTOM_NAME, Component.literal("Mutated caller object"));
        AnvilBrowserData.build("emi", fixture.access(), List.of(emiVariant, highBook));
        assertVariantPresent("JEI variant");
        assertVariantPresent("EMI variant");
        assertVariantAbsent("Mutated caller object");
        assertEquals(List.of(1, 2, 3, 255), AnvilEnchantData.levelsToRegister(fixture.enchantment));

        // Changes to derived stacks must not mutate the retained source copy either.
        findVariant("EMI variant").set(DataComponents.CUSTOM_NAME, Component.literal("Mutated derived object"));
        AnvilBrowserData.build("jei", fixture.access(), List.of(namedSword("Replacement JEI variant")));
        assertVariantAbsent("JEI variant");
        assertVariantPresent("Replacement JEI variant");
        assertVariantPresent("EMI variant");
        assertVariantAbsent("Mutated derived object");
        assertEquals(255, AnvilEnchantData.maxLevel(fixture.enchantment));

        AnvilBrowserData.remove("emi");
        assertVariantAbsent("EMI variant");
        assertVariantPresent("Replacement JEI variant");
        assertEquals(List.of(1, 2, 3), AnvilEnchantData.levelsToRegister(fixture.enchantment));
        assertTrue(AnvilEnchantData.observedBooks().isEmpty());
        AnvilBrowserData.remove("jei");
        assertTrue(AnvilEnchantData.enchantments().isEmpty());
        assertTrue(AnvilEnchantData.enchantableStacks().isEmpty());
    }

    @Test
    void aDifferentRegistryWithTheSameIdsCannotInheritOldBrowserSourcesOrObservedLevels() {
        Fixture first = new Fixture(3);
        AnvilBrowserData.build("jei", first.access(), List.of(namedSword("Old JEI variant")));
        AnvilBrowserData.build("emi", first.access(), List.of(namedSword("Old EMI variant"), book(first.enchantment, 255)));
        assertEquals(255, AnvilEnchantData.maxLevel(first.enchantment));

        Fixture second = new Fixture(1);
        assertEquals(first.enchantment, second.enchantment, "registered Holder equality alone cannot identify a connection");
        AnvilBrowserData.build("emi", second.access(), List.of(namedSword("New EMI variant")));
        assertVariantAbsent("Old JEI variant");
        assertVariantAbsent("Old EMI variant");
        assertVariantPresent("New EMI variant");
        assertEquals(List.of(1), AnvilEnchantData.levelsToRegister(second.enchantment));
        assertTrue(AnvilEnchantData.observedBooks().isEmpty());
        assertSame(second.enchantment, AnvilEnchantData.highestBooks(Items.DIAMOND_SWORD).getFirst().enchantment());

        // Removing an old provider is harmless and cannot clear the new connection's source.
        AnvilBrowserData.remove("jei");
        assertVariantPresent("New EMI variant");
        AnvilBrowserData.build("jei", second.access(), List.of(namedSword("New JEI variant")));
        assertVariantPresent("New EMI variant");
        assertVariantPresent("New JEI variant");
        AnvilBrowserData.reset();
        AnvilBrowserData.build("jei", second.access(), List.of());
        assertVariantAbsent("New EMI variant");
        assertVariantAbsent("New JEI variant");
    }

    private static ItemStack namedSword(String name) {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static ItemStack book(Holder<Enchantment> enchantment, int level) {
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(enchantment, level);
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        stack.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return stack;
    }

    private static ItemStack findVariant(String name) {
        return AnvilEnchantData.enchantableStacks().stream()
                .filter(stack -> Component.literal(name).equals(stack.get(DataComponents.CUSTOM_NAME)))
                .findFirst().orElseThrow();
    }

    private static void assertVariantPresent(String name) {
        assertTrue(AnvilEnchantData.enchantableStacks().stream()
                .anyMatch(stack -> Component.literal(name).equals(stack.get(DataComponents.CUSTOM_NAME))), name);
    }

    private static void assertVariantAbsent(String name) {
        assertFalse(AnvilEnchantData.enchantableStacks().stream()
                .anyMatch(stack -> Component.literal(name).equals(stack.get(DataComponents.CUSTOM_NAME))), name);
    }

    private static final class Fixture {
        private final MappedRegistry<Enchantment> registry = new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable());
        private final Holder<Enchantment> enchantment;

        Fixture(int maxLevel) {
            Enchantment.EnchantmentDefinition definition = new Enchantment.EnchantmentDefinition(
                    HolderSet.direct(Items.DIAMOND_SWORD.builtInRegistryHolder()), Optional.empty(),
                    10, maxLevel, new Enchantment.Cost(1, 0), new Enchantment.Cost(10, 0),
                    4, List.of(EquipmentSlotGroup.ANY));
            enchantment = registry.register(ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath("browser_test", "same_id")),
                    new Enchantment(Component.literal("Browser fixture"), definition, HolderSet.direct(), DataComponentMap.EMPTY),
                    RegistrationInfo.BUILT_IN);
        }

        RegistryAccess access() {
            return new RegistryAccess.ImmutableRegistryAccess(List.of(registry.freeze()));
        }
    }
}
