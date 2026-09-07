package com.atlas.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    private Path databasePath;
    private InventoryRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        databasePath = temporaryDirectory.resolve("inventory.db");
        repository = new InventoryRepository(databasePath);
        repository.initialize();
    }

    @Test
    void initializesSchemaAndSeedDataOnlyOnce() throws Exception {
        assertEquals(12, repository.findAll().size());

        repository.initialize();

        assertEquals(12, repository.findAll().size());
        assertTrue(repository.isHealthy());
    }

    @Test
    void createsAndFindsAnItem() throws Exception {
        InventoryItem created = repository.create(newItem("RES-10K", "10 kOhm Resistor"));

        assertTrue(created.id() > 0);
        assertEquals(created, repository.findById(created.id()).orElseThrow());
    }

    @Test
    void updatesAnItem() throws Exception {
        InventoryItem original = repository.create(newItem("CAP-100UF", "100 uF Capacitor"));
        InventoryItem changed = new InventoryItem(
                original.id(), "CAP-100UF", "100 uF Electrolytic Capacitor", "Capacitor",
                "C-07-03", 31, 12, "Radial lead component");

        assertTrue(repository.update(original.id(), changed));
        assertEquals(changed, repository.findById(original.id()).orElseThrow());
    }

    @Test
    void deletesAnItem() throws Exception {
        InventoryItem created = repository.create(newItem("LED-GRN-5MM", "Green LED"));

        assertTrue(repository.delete(created.id()));
        assertFalse(repository.findById(created.id()).isPresent());
        assertFalse(repository.delete(created.id()));
    }

    @Test
    void enforcesUniquePartNumbers() throws Exception {
        repository.create(newItem("RES-1K", "1 kOhm Resistor"));

        assertThrows(SQLException.class,
                () -> repository.create(newItem("RES-1K", "Duplicate Resistor")));
    }

    @Test
    void persistsDataAcrossRepositoryInstances() throws Exception {
        InventoryItem created = repository.create(newItem("DIO-1N4007", "Rectifier Diode"));

        InventoryRepository reopened = new InventoryRepository(databasePath);

        List<InventoryItem> items = reopened.findAll();
        assertEquals(13, items.size());
        assertEquals(created, reopened.findById(created.id()).orElseThrow());
    }

    private static InventoryItem newItem(String partNumber, String name) {
        return new InventoryItem(0, partNumber, name, "Component", "C-08-01",
                20, 5, "Test inventory item");
    }
}
