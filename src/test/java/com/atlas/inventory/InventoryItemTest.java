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

    @Test
    void isOutOfStockWhenQuantityIsZero() {
        assertEquals(InventoryItem.StockStatus.OUT_OF_STOCK, item(0, 5).stockStatus());
    }

    @Test
    void isLowWhenQuantityIsBelowReorderLevel() {
        assertEquals(InventoryItem.StockStatus.LOW, item(3, 5).stockStatus());
    }

    @Test
    void isLowWhenQuantityEqualsReorderLevel() {
        assertEquals(InventoryItem.StockStatus.LOW, item(5, 5).stockStatus());
    }

    @Test
    void isOkWhenQuantityIsAboveReorderLevel() {
        assertEquals(InventoryItem.StockStatus.OK, item(6, 5).stockStatus());
    }

    @Test
    void isOutOfStockWhenQuantityAndReorderLevelAreZero() {
        assertEquals(InventoryItem.StockStatus.OUT_OF_STOCK, item(0, 0).stockStatus());
    }

    @Test
    void stockLevelPercentageIsFullWhenItemHasNoReorderLevel() {
        assertEquals(100, item(12, 0).stockLevelPercentage());
    }

    @Test
    void stockLevelPercentageIsFullWhenEmptyItemHasNoReorderLevel() {
        assertEquals(100, item(0, 0).stockLevelPercentage());
    }

    @Test
    void stockLevelPercentageIsFiftyWhenQuantityIsHalfTheReorderLevel() {
        assertEquals(50, item(5, 10).stockLevelPercentage());
    }

    @Test
    void stockLevelPercentageIsFullWhenQuantityEqualsReorderLevel() {
        assertEquals(100, item(10, 10).stockLevelPercentage());
    }

    @Test
    void stockLevelPercentageCanExceedOneHundred() {
        assertEquals(200, item(20, 10).stockLevelPercentage());
    }

    private static InventoryItem item(int quantity, int reorderLevel) {
        return new InventoryItem(1, "TEST-ITEM", "Test Item", "Test", "A-01",
                quantity, reorderLevel, "Test inventory item");
    }
}
