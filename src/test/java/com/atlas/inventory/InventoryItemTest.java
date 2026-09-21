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

}
