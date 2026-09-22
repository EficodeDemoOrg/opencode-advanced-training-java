package com.atlas.inventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryItemTest {

    @Test
    void shortageWhenQuantityIsBelowReorderLevel() {
        InventoryItem item = new InventoryItem(0, "A-1", "Short Item", "Component", "A-01-01",
                2, 10, "");

        assertEquals(8, item.reorderShortage());
    }

    @Test
    void noShortageWhenQuantityEqualsReorderLevel() {
        InventoryItem item = new InventoryItem(0, "A-2", "Even Item", "Component", "A-01-02",
                10, 10, "");

        assertEquals(0, item.reorderShortage());
    }

    @Test
    void noShortageWhenQuantityIsAboveReorderLevel() {
        InventoryItem item = new InventoryItem(0, "A-3", "Surplus Item", "Component", "A-02-01",
                25, 10, "");

        assertEquals(0, item.reorderShortage());
    }

    @Test
    void noShortageWhenBothValuesAreZero() {
        InventoryItem item = new InventoryItem(0, "A-4", "Zero Item", "Component", "A-03-01",
                0, 0, "");

        assertEquals(0, item.reorderShortage());
    }

    @Test
    void outOfStockWhenQuantityIsZero() {
        InventoryItem item = new InventoryItem(0, "B-1", "Empty Item", "Component", "A-04-01",
                0, 5, "");

        assertEquals(InventoryItem.StockStatus.OUT_OF_STOCK, item.stockStatus());
    }

    @Test
    void lowWhenQuantityIsBelowReorderLevel() {
        InventoryItem item = new InventoryItem(0, "B-2", "Low Item", "Component", "A-04-02",
                3, 5, "");

        assertEquals(InventoryItem.StockStatus.LOW, item.stockStatus());
    }

    @Test
    void lowWhenQuantityEqualsReorderLevel() {
        InventoryItem item = new InventoryItem(0, "B-3", "Even Item", "Component", "A-04-03",
                5, 5, "");

        assertEquals(InventoryItem.StockStatus.LOW, item.stockStatus());
    }

    @Test
    void okWhenQuantityIsAboveReorderLevel() {
        InventoryItem item = new InventoryItem(0, "B-4", "Full Item", "Component", "A-04-04",
                6, 5, "");

        assertEquals(InventoryItem.StockStatus.OK, item.stockStatus());
    }

    @Test
    void outOfStockWhenBothValuesAreZero() {
        InventoryItem item = new InventoryItem(0, "B-5", "Untracked Item", "Component", "A-04-05",
                0, 0, "");

        assertEquals(InventoryItem.StockStatus.OUT_OF_STOCK, item.stockStatus());
    }

    @Test
    void stockLevelPercentageHandlesNoReorderLevel() {
        InventoryItem item = new InventoryItem(
                0, "A-1", "No Reorder Tracking", "Component", "A-01-01", 12, 0, "");

        assertEquals(100, item.stockLevelPercentage());
    }

    @Test
    void stockLevelPercentageIsFullWhenQuantityIsZeroAndNoReorderLevel() {
        InventoryItem item = new InventoryItem(
                0, "C-2", "Untracked Empty Item", "Component", "C-02-01", 0, 0, "");

        assertEquals(100, item.stockLevelPercentage());
    }

    @Test
    void stockLevelPercentageIsHalfWhenQuantityIsHalfOfReorderLevel() {
        InventoryItem item = new InventoryItem(
                0, "C-3", "Half Stocked Item", "Component", "C-03-01", 5, 10, "");

        assertEquals(50, item.stockLevelPercentage());
    }

    @Test
    void stockLevelPercentageIsFullWhenQuantityEqualsReorderLevel() {
        InventoryItem item = new InventoryItem(
                0, "C-4", "Even Item", "Component", "C-04-01", 10, 10, "");

        assertEquals(100, item.stockLevelPercentage());
    }

    @Test
    void stockLevelPercentageMayExceedHundredWhenQuantityExceedsReorderLevel() {
        InventoryItem item = new InventoryItem(
                0, "C-5", "Surplus Item", "Component", "C-05-01", 20, 10, "");

        assertEquals(200, item.stockLevelPercentage());
    }

}
