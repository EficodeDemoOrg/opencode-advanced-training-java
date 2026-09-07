package com.atlas.inventory;

import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        int port = readPort(System.getenv("PORT"));
        Path databasePath = Path.of(System.getenv().getOrDefault(
                "INVENTORY_DB", "data/inventory.db"));

        InventoryRepository repository = new InventoryRepository(databasePath);
        repository.initialize();

        InventoryServer server = new InventoryServer(port, new InventoryService(repository));
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();

        System.out.printf("Atlas Inventory is running at http://localhost:%d%n", server.port());
    }

    private static int readPort(String value) {
        if (value == null || value.isBlank()) {
            return 8080;
        }
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65_535) {
                throw new IllegalArgumentException("PORT must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("PORT must be a number", exception);
        }
    }
}
