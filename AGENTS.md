# AGENTS.md

Atlas Inventory is a sample app for an OpenCode agentic-coding workshop, not a production project. This repository is a fork of the intermediate training repo (currently identical to it), with the goal of landing all intermediate exercises so the default state becomes the starting point for future advanced exercises. Workshop exercises live in `exercises/`; when a task references an exercise number, start with that exercise file.

## Commands

- Full build + all tests + jar: `mvn clean verify` (CI runs `mvn --batch-mode verify` on push/PR; Java 17, Maven 3.9+).
- Single test class: `mvn test -Dtest=InventoryServiceTest` (single-module repo; no `-pl` needed).
- Run: `java -jar target/inventory-app.jar` → http://localhost:8080 (bound to loopback only). The shaded jar's finalName is `inventory-app`, not the versioned name.
- Env vars: `PORT` (default 8080), `INVENTORY_DB` (default `data/inventory.db`). The app creates the DB file and parent directory itself and seeds from `db/seed.sql` when the table is empty. Reset: stop the app, delete the `.db`, restart.
- Baseline: 15 tests in 3 classes (Repository 6, Service 2, Server 7), all green on `main`.

## Exercises and planted defects

The intermediate exercises (`exercises/fundamentals/hands-on/01`–`05`) are not implemented in this repo yet. Do not implement them or "fix" the planted defects preemptively — only when explicitly asked. When asked, follow the exercise files; their deliberate ambiguities are decisions for the user, not the agent.

- `InventoryService.validateForUpdate` deliberately does **not** check `quantity`, so `PUT` accepts negative stock (create does check it). No test covers it. When refactoring, do not unify `validateForCreate`/`validateForUpdate` into one shared validator without preserving this asymmetry — the suite stays green either way, so a green build proves nothing about this change.
- `exercises/scripts/apply-exercise-03-defect.{sh,ps1}` (run from repo root, idempotent) inject a static `totalReorderShortage` into `InventoryService.java` (unclamped sum; returns -37 where 8 is expected) plus a failing test in `InventoryServiceTest.java`. Exercise 3 is to fix it. On the `exercise-03` branch CI is red **by design** — never merge that branch to `main`.

### Target end state once the exercises are implemented

- Exercise 2: `InventoryItem.reorderShortage()` and a new `InventoryItemTest.java`.
- Exercise 3: `totalReorderShortage` clamped (e.g. `Math.max(0, …)`) so its regression test passes.
- Exercise 4: the duplicated reorder-level check factored into a shared helper, with `validateForUpdate`'s missing quantity check preserved.
- Exercise 5: tasks 5.A–5.E — maximum `quantity` on create, stock status on `InventoryItem`, `stockLevelPercentage` divide-by-zero, description boundary tests, and a 409 message that includes the part number.

## Testing gotchas

- Never use an in-memory SQLite database: `InventoryRepository` opens a fresh JDBC connection per operation, so `:memory:` is empty again by the time the query runs (`no such table: inventory_items`). Use a real temp file — copy the `@TempDir` + `repository.initialize()` pattern from `InventoryServiceTest`.
- `InventoryServerTest#reportsDuplicatePartNumbersAsConflict` asserts the 409 body contains the exact substring `part number already exists`. Appending to that message is fine; replacing it breaks the test.
- `InventoryItemTest` is deliberately **not** shipped in the current state — creating it is part of exercise 2. Do not assume it exists until that exercise has been landed.

## API shape (counterintuitive)

- Responses are JSON, but POST/PUT request bodies must be `application/x-www-form-urlencoded` (HTML form format, not JSON); the server rejects anything else with 400.
- `PUT /api/items/{id}` replaces the whole record — omitted fields are treated as missing, not preserved.
- Errors are `{"error":"message"}` with 400 (bad input), 404, 405, 409 (duplicate part number), 500, 503 (health only).

## Architecture

- Single-module Maven; no framework, ORM, or frontend build tool: JDK `HttpServer` + SQLite JDBC + a vanilla JS frontend.
- `Main` (env, startup) → `InventoryServer` (routing, form parsing, loopback bind) → `InventoryService` (validation; `InventoryException.ErrorType` maps to 400/404/409) → `InventoryRepository` (SQLite, one connection per operation). `InventoryItem` is a record with a hand-rolled `toJson()`.
- Frontend is `src/main/resources/static/`, served from the same server. Summary metrics — including "storage zones" via `storageLocation.split("-")[0]` — are computed client-side in `app.js`, not in Java.

## Workshop conventions

- `exercises/TRAINER-NOTES.md` is trainer-only and documents the planted defects and exercise traps. Do not use it to solve an exercise you are running.
- This `AGENTS.md` is temporary: it guides the work of landing the intermediate exercises. Once they are implemented and the advanced exercises are added, the file is removed so advanced-exercise participants start from a fresh state with no pre-existing instructions. Keep it accurate if the codebase changes before then.
- `opencode.json` pre-allows `mvn test*` and `mvn clean verify*`; destructive commands (`rm`, `git push`, `git reset --hard`, …) are denied and other bash prompts for approval.
