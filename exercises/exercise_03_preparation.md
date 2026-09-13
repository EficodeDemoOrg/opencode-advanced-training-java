# Trainer setup (do this before the exercise starts)

This exercise needs a real, reproducible defect in the repository: a buggy
`totalReorderShortage` method in
[`src/main/java/com/atlas/inventory/InventoryService.java`](../src/main/java/com/atlas/inventory/InventoryService.java)
plus a failing regression test for it in
`src/test/java/com/atlas/inventory/InventoryServiceTest.java`.

## Recommended: run the setup script

[`exercises/scripts/apply-exercise-03-defect.sh`](scripts/apply-exercise-03-defect.sh)
(bash) and
[`exercises/scripts/apply-exercise-03-defect.ps1`](scripts/apply-exercise-03-defect.ps1)
(PowerShell) apply both edits, confirm the new test fails with the expected
`-37` instead of `8`, and can create the branch and commit for you. Run one of
these from the repository root:

```bash
./exercises/scripts/apply-exercise-03-defect.sh --branch exercise-03 --commit
```

```powershell
./exercises/scripts/apply-exercise-03-defect.ps1 -Branch exercise-03 -Commit
```

Both scripts are idempotent — running one twice, or against a checkout that
already has the defect, does nothing the second time. Omit `--branch`/`-Branch`
and `--commit`/`-Commit` to only edit the two files in your current working
tree, and handle git yourself.

**How to get this onto every attendee's machine at once:** push the resulting
`exercise-03` branch to the repository attendees cloned from. Everyone gets
the identical defect with one command, run at the moment you say go:

```bash
git push -u origin exercise-03
```

```bash
# each attendee runs, once you say go:
git fetch origin
git checkout exercise-03
```

This only works cleanly if attendees haven't diverged in the two files the
script touches. They haven't at this point in the workshop: exercise 1 is
read-only, and exercise 2 only touches `InventoryItem.java` and
`InventoryItemTest.java`. If an attendee's `InventoryService.java` or
`InventoryServiceTest.java` differ from origin's `main` for any other reason
(a false start, a typo fixed by hand), `git checkout exercise-03` will refuse
to overwrite uncommitted local changes rather than silently discard them —
have them `git stash` or commit first.

If your attendees don't share one fetchable git remote (personal forks,
offline clones), skip the branch entirely: distribute the script itself (repo
file, Slack, shared drive) and have everyone run it locally at the same time
instead. The script's own output is your verification — if it doesn't print
"Confirmed: ... fails with -37 instead of 8", something about that machine's
checkout differs from what the script expects, and the person running it will
see that immediately rather than after they start exercise 3.

## What the script does, if you want to apply it by hand instead

Add the following buggy method to `InventoryService.java` (for example, near
`isHealthy()`):

```java
public static int totalReorderShortage(List<InventoryItem> items) {
    int total = 0;
    for (InventoryItem item : items) {
        total += item.reorderLevel() - item.quantity();
    }
    return total;
}
```

Then add this failing test to the existing `InventoryServiceTest.java` (add
`import java.util.List;` alongside the imports already there):

```java
    @Test
    void totalShortageAcrossMultipleItemsIgnoresItemsAboveReorderLevel() {
        InventoryItem shortItem = new InventoryItem(
                0, "A-1", "Short Item", "Component", "A-01-01", 2, 10, "");
        InventoryItem surplusItem = new InventoryItem(
                0, "B-1", "Surplus Item", "Component", "A-02-01", 50, 5, "");

        int total = InventoryService.totalReorderShortage(List.of(shortItem, surplusItem));

        assertEquals(8, total);
    }
```

Run `mvn test -Dtest=InventoryServiceTest` and confirm this test fails (`total`
comes out to `-37`, not `8`, because the surplus item's negative contribution
cancels out part of the real shortage). The two tests already in the file should
still pass.

Commit this state on a dedicated branch before attendees start:

```bash
git checkout -b exercise-03
git add src/main/java/com/atlas/inventory/InventoryService.java \
        src/test/java/com/atlas/inventory/InventoryServiceTest.java
git commit -m "Add failing totalReorderShortage test for exercise 3"
```

If you push that branch, `.github/workflows/build.yml` runs `mvn --batch-mode
verify` on it and CI will go **red by design** — that's the point of the
exercise, not a setup mistake. Don't merge the branch to `main`.
