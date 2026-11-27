package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.List;
import java.util.Optional;

import com.mysterious.mystic.compat.anvilguide.data.AnvilEnchantData;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilItemEnchantRecipe;
import com.mysterious.mystic.compat.anvilguide.recipe.AnvilRecipeFactory;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Shared item-page eligibility with real repair rules; this does not launch an EMI R/U screen. */
class AnvilItemInformationTest {
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
    void aNonEnchantableRepairableItemHasAnInformationPageFromTheRegisteredRepairIndex() {
        AnvilEnchantData.build(fixtureRegistry(), List.of());
        ItemStack armor = new ItemStack(Items.WOLF_ARMOR);
        assertFalse(armor.getItem().isEnchantable(armor), "wolf armor is intrinsically non-enchantable");
        assertTrue(AnvilEnchantData.enchantableStacks().stream().noneMatch(stack -> stack.is(Items.WOLF_ARMOR)));

        AnvilItemEnchantRecipe page = AnvilRecipeFactory.itemRecipe(armor, false);
        assertTrue(page.books().isEmpty());
        assertFalse(page.expandable());
        assertTrue(AnvilEnchantData.repairMaterials(armor).stream().anyMatch(stack -> stack.is(Items.ARMADILLO_SCUTE)));
        assertTrue(AnvilEnchantData.repairGroups().stream().anyMatch(group ->
                group.items().stream().anyMatch(stack -> stack.is(Items.WOLF_ARMOR))
                        && group.materials().stream().anyMatch(stack -> stack.is(Items.ARMADILLO_SCUTE))),
                "the replacement can register repair-only equipment outside the enchantable sample list");
        assertTrue(AnvilRecipeFactory.hasItemInformation(page));
    }

    @Test
    void aLateRepairOnlyVariantRetainsItsNameDamageAndCustomStateWithoutRebuildingTheRegistry() {
        AnvilEnchantData.build(fixtureRegistry(), List.of());
        int generation = AnvilEnchantData.generation();
        ItemStack armor = new ItemStack(Items.WOLF_ARMOR);
        armor.setDamageValue(15);
        armor.set(DataComponents.CUSTOM_NAME, Component.literal("Late wolf armor variant"));
        CompoundTag state = new CompoundTag();
        state.putInt("energy", 512);
        armor.set(DataComponents.CUSTOM_DATA, CustomData.of(state));

        AnvilItemEnchantRecipe page = AnvilRecipeFactory.itemRecipe(armor, false);
        assertTrue(page.books().isEmpty());
        assertTrue(AnvilRecipeFactory.hasItemInformation(page));
        assertTrue(ItemStack.isSameItemSameComponents(armor, page.input()));
        assertEquals(15, page.input().getDamageValue());
        assertEquals("Late wolf armor variant", page.input().getHoverName().getString());
        assertEquals(armor.get(DataComponents.CUSTOM_DATA), page.input().get(DataComponents.CUSTOM_DATA));
        assertEquals(generation, AnvilEnchantData.generation(), "a focused repair-only query reuses the current index");
    }

    @Test
    void enchantmentOnlyContentStillQualifiesWhileItemsWithNoInformationDoNot() {
        AnvilEnchantData.build(fixtureRegistry(), List.of());
        ItemStack trident = new ItemStack(Items.TRIDENT);
        AnvilItemEnchantRecipe enchantmentPage = AnvilRecipeFactory.itemRecipe(trident, false);
        assertFalse(enchantmentPage.books().isEmpty(), "the fixture supports an ordinary trident book");
        assertTrue(AnvilEnchantData.repairMaterials(trident).isEmpty(), "a trident has no vanilla material repair");
        assertTrue(AnvilRecipeFactory.hasItemInformation(enchantmentPage));

        ItemStack paper = new ItemStack(Items.PAPER);
        assertFalse(AnvilRecipeFactory.hasItemInformation(AnvilRecipeFactory.itemRecipe(paper, false)));
        assertFalse(AnvilRecipeFactory.hasItemInformation(AnvilRecipeFactory.itemRecipe(ItemStack.EMPTY, false)));
    }

    @Test
    void aDisconnectedRegistryCannotKeepAnOldRepairOnlyPageEligible() {
        ItemStack armor = new ItemStack(Items.WOLF_ARMOR);
        assertFalse(AnvilRecipeFactory.hasItemInformation(AnvilRecipeFactory.itemRecipe(armor, false)));
        AnvilEnchantData.build(fixtureRegistry(), List.of());
        AnvilItemEnchantRecipe page = AnvilRecipeFactory.itemRecipe(armor, false);
        assertTrue(AnvilRecipeFactory.hasItemInformation(page));

        AnvilEnchantData.reset();
        assertFalse(AnvilRecipeFactory.hasItemInformation(page), "repair data belongs to the active connection");
        assertFalse(AnvilRecipeFactory.hasItemInformation(AnvilRecipeFactory.itemRecipe(armor, false)));
    }

    private static RegistryAccess fixtureRegistry() {
        MappedRegistry<Enchantment> registry = new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable());
        Enchantment.EnchantmentDefinition definition = new Enchantment.EnchantmentDefinition(
                HolderSet.direct(Items.TRIDENT.builtInRegistryHolder()), Optional.empty(),
                10, 3, new Enchantment.Cost(1, 0), new Enchantment.Cost(10, 0),
                4, List.of(EquipmentSlotGroup.ANY));
        registry.register(ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath("information_test", "trident")),
                new Enchantment(Component.literal("Fixture trident enchantment"), definition,
                        HolderSet.direct(), DataComponentMap.EMPTY), RegistrationInfo.BUILT_IN);
        return new RegistryAccess.ImmutableRegistryAccess(List.of(registry.freeze()));
    }
}
