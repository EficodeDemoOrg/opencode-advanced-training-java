# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

Atlas Inventory is a small, deliberately framework-free Java hardware inventory app. Its purpose is to serve as sample code for an agentic-coding training workshop (originally built for [opencode](https://opencode.ai)) — not to be a production system. It provides a browser UI and an HTTP API for viewing, adding, updating, and removing component records.

Tech stack, deliberately minimal:
- Java 17, JDK `HttpServer` (no web framework)
- SQLite via JDBC (no ORM)
- Vanilla HTML/CSS/JS (no frontend build tool)
- Maven + JUnit 5

## Commands

Build, run all tests, and produce the jar:
```bash
mvn clean verify
```

Run tests only:
```bash
mvn test
```

Run a single test class:
```bash
mvn test -Dtest=InventoryServiceTest
```

Run a single test method:
```bash
mvn test -Dtest=InventoryServiceTest#methodName
```

Package without cleaning:
```bash
mvn package
```

Run the app (after building):
```bash
java -jar target/inventory-app.jar
```
Listens on `http://localhost:8080` (loopback only). Configurable via env vars `PORT` (default `8080`) and `INVENTORY_DB` (default `data/inventory.db`). The database and its parent directory are created and seeded automatically on first run if the inventory table is empty.

CI (`.github/workflows/build.yml`) runs `mvn --batch-mode verify` on every push/PR.

## Architecture

Four classes in `com.atlas.inventory`, each with a single layer of responsibility — read all of them before making cross-cutting changes:

- **`Main`** — reads env config, wires `InventoryRepository` → `InventoryService` → `InventoryServer`, starts the server.
- **`InventoryRepository`** — all SQLite access (JDBC, hand-written SQL, no connection pooling — a new `Connection` is opened per call). Also owns schema/seed initialization (`src/main/resources/db/schema.sql`, `seed.sql`).
- **`InventoryService`** — validation and business rules; the only layer that should throw `InventoryService.InventoryException` (carries an `ErrorType`: `INVALID_INPUT`, `NOT_FOUND`, `CONFLICT`). Translates SQLite unique-constraint failures (`part_number`) into `CONFLICT`.
- **`InventoryServer`** — hand-rolled HTTP routing on top of `com.sun.net.httpserver.HttpServer`. Parses `application/x-www-form-urlencoded` bodies itself, builds JSON by hand (no Jackson/Gson), maps exception types to HTTP status codes in one place (`handle()`), and serves the static frontend (`src/main/resources/static/`) for unmatched GET paths.
- **`InventoryItem`** — an immutable record; also owns its own hand-written JSON serialization (`toJson()`).

There is no framework "magic" anywhere — routing, form parsing, JSON encoding, and DB access are all explicit and readable top-to-bottom in these four files. When extending behavior, follow the existing layering (repository → service → server) rather than introducing new abstractions.

### Known deliberate gap
Quantity non-negativity is validated on create (`InventoryService.validateForCreate`) but **not** on update (`validateForUpdate`), so `PUT` can persist a negative quantity. This is intentional — used by the workshop exercises — do not "fix" it unless a task explicitly asks for it.

### Tests
One test class per main class, in `src/test/java/com/atlas/inventory/`: `InventoryRepositoryTest`, `InventoryServiceTest`, `InventoryServerTest` (spins up a real server on an ephemeral port with a real HTTP client against a `@TempDir` SQLite file — no mocking).

## Workshop exercises

`exercises/` contains the training curriculum, meant to be read/run in order — see `exercises/README.md`. Exercises 3 and 5 expect a trainer-injected defect (a buggy `totalReorderShortage` method + failing test in `InventoryService`/`InventoryServiceTest`); see `exercises/exercise_03_preparation.md` and `exercises/scripts/apply-exercise-03-defect.{sh,ps1}`. Do not apply or remove that defect unless explicitly asked — it's exercise scaffolding, not a real bug to fix opportunistically.
