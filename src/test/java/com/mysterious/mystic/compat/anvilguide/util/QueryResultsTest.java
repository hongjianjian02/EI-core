package com.mysterious.mystic.compat.anvilguide.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryResultsTest {
    @Test
    void focusedPagesReplaceEveryOwnedIndexCopyAndKeepOtherMachinesInOrder() {
        Page machine = new Page("machine", false);
        Page crafting = new Page("crafting", false);
        List<Page> original = List.of(new Page("indexed level 1", true), machine,
                new Page("indexed level 255", true), crafting);
        Page focused = new Page("exact multi-enchantment book", true);
        List<Page> merged = QueryResults.replace(original, Page::owned, List.of(focused));

        assertEquals(List.of(machine, crafting, focused), merged);
        assertSame(machine, merged.getFirst());
        assertEquals(4, original.size(), "the provider's immutable result must be left intact");
        assertThrows(UnsupportedOperationException.class, () -> merged.add(machine));
    }

    @Test
    void anInvalidOldWorldFocusRemovesOnlyOwnedPages() {
        Page machine = new Page("other mod anvil machine", false);
        List<Page> merged = QueryResults.replace(List.of(new Page("stale guide", true), machine),
                Page::owned, List.of());
        assertEquals(List.of(machine), merged);
    }

    @Test
    void aMaterialCanContributeRepairAndEnchantViewsWithoutMultiplyingGridPages() {
        Page machine = new Page("machine", false);
        Page item = new Page("one item guide", true);
        Page repair = new Page("one material group", true);
        assertEquals(List.of(machine, item, repair), QueryResults.replace(List.of(machine),
                Page::owned, List.of(item, repair)));
    }

    private record Page(String name, boolean owned) {
    }
}
