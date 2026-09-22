package com.atlas.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Service-level tests.
 *
 * <p>{@code InventoryService} needs a real {@link InventoryRepository} behind it, so every test
 * here runs against a throwaway SQLite file in a JUnit {@code @TempDir}. Reuse the setUp() below
 * when you add tests in the exercises — do not reach for an in-memory database, because this
 * repository opens a fresh connection per operation and an in-memory one would be empty again by
 * the time the query runs.
 */
class InventoryServiceTest {
    @TempDir
    Path temporaryDirectory;

    private InventoryService service;

    @BeforeEach
    void setUp() throws Exception {
        InventoryRepository repository =
                new InventoryRepository(temporaryDirectory.resolve("inventory.db"));
        repository.initialize();
        service = new InventoryService(repository);
    }

    @Test
    void createsAnItemAndReadsItBack() throws Exception {
        InventoryItem created = service.create(newItem("RES-10K", 100));

        assertEquals("RES-10K", service.findById(created.id()).partNumber());
        assertEquals(100, service.findById(created.id()).quantity());
    }

    @Test
    void rejectsANegativeQuantityOnCreate() {
        InventoryService.InventoryException failure = assertThrows(
                InventoryService.InventoryException.class,
                () -> service.create(newItem("RES-1K", -1)));

        assertEquals(InventoryService.ErrorType.INVALID_INPUT, failure.type());
        assertEquals("Quantity cannot be negative", failure.getMessage());
    }

    @Test
    void acceptsAQuantityAtTheMaximumOnCreate() throws Exception {
        InventoryItem created = service.create(newItem("CAP-1", 100000));

        assertEquals(100000, created.quantity());
    }

    @Test
    void rejectsAQuantityAboveTheMaximumOnCreate() {
        InventoryService.InventoryException failure = assertThrows(
                InventoryService.InventoryException.class,
                () -> service.create(newItem("CAP-2", 100001)));

        assertEquals(InventoryService.ErrorType.INVALID_INPUT, failure.type());
        assertEquals("Quantity cannot be greater than 100000", failure.getMessage());
    }

    @Test
    void acceptsAZeroQuantityOnCreate() throws Exception {
        InventoryItem created = service.create(newItem("CAP-3", 0));

        assertEquals(0, created.quantity());
    }

    @Test
    void doesNotEnforceTheQuantityMaximumOnUpdate() throws Exception {
        InventoryItem created = service.create(newItem("CAP-4", 100));

        InventoryItem updated = service.update(created.id(),
                new InventoryItem(created.id(), "CAP-4", "Test Component", "Component",
                        "C-08-01", 100001, 5, "Updated during a service test"));

        assertEquals(100001, updated.quantity());
    }

    @Test
    void acceptsADescriptionAtTheMaximumLengthOnCreate() throws Exception {
        InventoryItem created = service.create(newItemWithDescription("TXT-1", "d".repeat(500)));

        assertEquals(500, created.description().length());
    }

    @Test
    void rejectsADescriptionAboveTheMaximumLengthOnCreate() {
        InventoryService.InventoryException failure = assertThrows(
                InventoryService.InventoryException.class,
                () -> service.create(newItemWithDescription("TXT-2", "d".repeat(501))));

        assertEquals(InventoryService.ErrorType.INVALID_INPUT, failure.type());
        assertEquals("Description must be 500 characters or fewer", failure.getMessage());
    }

    @Test
    void acceptsAnEmptyDescriptionOnCreate() throws Exception {
        InventoryItem created = service.create(newItemWithDescription("TXT-3", ""));

        assertEquals("", created.description());
    }

    @Test
    void totalShortageAcrossMultipleItemsIgnoresItemsAboveReorderLevel() {
        InventoryItem shortItem = new InventoryItem(
                0, "A-1", "Short Item", "Component", "A-01-01", 2, 10, "");
        InventoryItem surplusItem = new InventoryItem(
                0, "B-1", "Surplus Item", "Component", "A-02-01", 50, 5, "");

        int total = InventoryService.totalReorderShortage(List.of(shortItem, surplusItem));

        assertEquals(8, total);
    }

    private static InventoryItem newItem(String partNumber, int quantity) {
        return new InventoryItem(0, partNumber, "Test Component", "Component", "C-08-01",
                quantity, 5, "Created during a service test");
    }

    private static InventoryItem newItemWithDescription(String partNumber, String description) {
        return new InventoryItem(0, partNumber, "Test Component", "Component", "C-09-01",
                10, 5, description);
    }
}
