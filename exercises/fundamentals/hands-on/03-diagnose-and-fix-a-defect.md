# Exercise 3: Diagnose and Fix a Defect (Live) — 25 minutes

## Trainer setup (do this before the exercise starts)

This exercise needs a real, reproducible defect in the repository. Add the
following buggy method to
[`src/main/java/com/atlas/inventory/InventoryService.java`](../../src/main/java/com/atlas/inventory/InventoryService.java)
(for example, near `isHealthy()`):

```java
public static int totalReorderShortage(List<InventoryItem> items) {
    int total = 0;
    for (InventoryItem item : items) {
        total += item.reorderLevel() - item.quantity();
    }
    return total;
}
```

Then add a new test file,
`src/test/java/com/atlas/inventory/InventoryServiceTest.java`, with this failing
test:

```java
package com.atlas.inventory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryServiceTest {
    @Test
    void totalShortageAcrossMultipleItemsIgnoresItemsAboveReorderLevel() {
        InventoryItem shortItem = new InventoryItem(
                0, "A-1", "Short Item", "Component", "A-01-01", 2, 10, "");
        InventoryItem surplusItem = new InventoryItem(
                0, "B-1", "Surplus Item", "Component", "A-02-01", 50, 5, "");

        int total = InventoryService.totalReorderShortage(List.of(shortItem, surplusItem));

        assertEquals(8, total);
    }
}
```

Run `mvn test -Dtest=InventoryServiceTest` and confirm it fails (`total` comes
out to `-37`, not `8`, because the surplus item's negative contribution
cancels out part of the real shortage). Commit this state on the exercise
branch before attendees start.

## Scenario (student-facing)

A test is failing in the inventory service. Give attendees only this defect
report — not the buggy code above:

> `InventoryServiceTest#totalShortageAcrossMultipleItemsIgnoresItemsAboveReorderLevel`
> is failing. `InventoryService.totalReorderShortage` is supposed to sum how
> many units are needed across a list of items to bring every item up to its
> reorder level. Items that already have enough stock should not affect the
> total. With one item short by 8 units and another item 45 units above its
> reorder level, the method returns `-37` instead of `8`.

## Prompt template

```
a test is failing in the inventory service.

investigate the failure before changing code.
please:
1. run the smallest relevant test (InventoryServiceTest).
2. identify the incorrect assumption or calculation in totalReorderShortage.
3. explain the expected versus actual result.
4. propose the smallest fix.

do not edit files yet.
do not inspect unrelated packages or directories such as InventoryRepository
or InventoryServer.
```

Then, after reviewing the diagnosis:

```
apply only the proposed fix.
add or adjust a focused regression test if necessary.
run the relevant test again and show the final diff summary.
```

## What you should expect

- The bug: each item's contribution to the total is `reorderLevel - quantity`
  without clamping negative values (surplus items) to `0`. A correct
  implementation clamps each item's individual shortfall at `0` before summing
  — for example `Math.max(0, item.reorderLevel() - item.quantity())`.
- The fix is one line inside the loop.
- `mvn test -Dtest=InventoryServiceTest` should pass afterward.

## Teaching points

- The agent should reproduce the problem (run the failing test) before
  proposing a fix, not just read the code and guess.
- A passing test afterward does not prove the diagnosis was correct — check
  that the agent's explanation of *why* the bug occurred matches the actual
  code, not just that the assertion now succeeds.
- Distinguish symptom removal (hardcoding `8` or special-casing the two test
  items) from correcting the underlying rule (clamping every item's shortfall
  at zero, which fixes the calculation for any input).
- Small models benefit from explicit expected-versus-actual values — the
  defect report above gives both `-37` (actual) and `8` (expected) on purpose.
