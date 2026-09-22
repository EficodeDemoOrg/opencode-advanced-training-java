# Repository Guide

## Build and Verification

- Use JDK 17+ and Maven 3.9+; there is no Maven wrapper.
- Run one test class while iterating: `mvn test -Dtest=InventoryServiceTest` (substitute the class name). Run one method with `mvn test -Dtest=InventoryServiceTest#methodName`.
- Run `mvn clean verify` before finishing broad changes. CI uses Java 17 and `mvn --batch-mode verify`.
- `mvn package` creates the shaded executable `target/inventory-app.jar`; run it with `java -jar target/inventory-app.jar`.
- No separate lint, formatter, typecheck, frontend build, or code-generation step is configured.

## Architecture

- This is one framework-free Maven module. `Main` wires `InventoryRepository -> InventoryService -> InventoryServer`; keep persistence, validation, and HTTP concerns in those respective layers.
- `InventoryServer` uses JDK `HttpServer` and manually handles routing, form decoding, and JSON. POST/PUT bodies are `application/x-www-form-urlencoded`, not JSON; `PUT` replaces the complete item.
- Browser assets under `src/main/resources/static/` are classpath resources served by explicit routes in `InventoryServer`; adding an asset file alone does not expose it.
- `InventoryRepository.initialize()` applies `db/schema.sql` and runs `db/seed.sql` whenever the table is empty. Schema changes must remain compatible with this startup path and its naive semicolon-delimited script execution.

## Tests and Data

- Repository, service, and server tests are integration-style tests using temporary on-disk SQLite databases. Do not replace them with `jdbc:sqlite::memory:`: the repository opens a new connection for each operation, so separate in-memory connections do not share state.
- Server tests bind to port `0` and call `server.port()`; do not hard-code a test port.
- The runtime database defaults to ignored path `data/inventory.db`; `INVENTORY_DB` overrides it and `PORT` defaults to `8080`. Never use the runtime database in tests.
- The update path currently permits negative `quantity` while create rejects it. This is a documented workshop defect; change it only when the requested task addresses that behavior.

## Workshop Context

- `exercises/` is course material, not application runtime code. Some trainer exercises intentionally introduce failing tests or defects; do not infer the desired production behavior from a prepared exercise branch without checking the task and `README.md`.
