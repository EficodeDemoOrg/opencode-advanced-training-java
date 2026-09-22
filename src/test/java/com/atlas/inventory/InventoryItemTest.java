package com.atlas.inventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryItemTest {
    @Test
    void calculatesShortageWhenQuantityIsBelowReorderLevel() {
        assertEquals(8, item(2, 10).reorderShortage());
    }

    @Test
    void hasNoShortageWhenQuantityEqualsReorderLevel() {
        assertEquals(0, item(10, 10).reorderShortage());
    }

    @Test
    void hasNoShortageWhenQuantityIsAboveReorderLevel() {
        assertEquals(0, item(25, 10).reorderShortage());
    }

    @Test
    void hasNoShortageWhenQuantityAndReorderLevelAreZero() {
        assertEquals(0, item(0, 0).reorderShortage());
    }

    private static InventoryItem item(int quantity, int reorderLevel) {
        return new InventoryItem(1, "TEST-ITEM", "Test Item", "Test", "A-01",
                quantity, reorderLevel, "Test inventory item");
    }
}
