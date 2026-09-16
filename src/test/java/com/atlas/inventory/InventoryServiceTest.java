package com.atlas.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

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

    private static InventoryItem newItem(String partNumber, int quantity) {
        return new InventoryItem(0, partNumber, "Test Component", "Component", "C-08-01",
                quantity, 5, "Created during a service test");
    }
}
