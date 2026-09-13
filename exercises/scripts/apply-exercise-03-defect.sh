#!/usr/bin/env bash
# Applies the exercise-03 defect (see exercises/exercise_03_preparation.md) to
# this checkout: a buggy totalReorderShortage() method in InventoryService.java,
# plus its failing regression test in InventoryServiceTest.java.
#
# Idempotent — running it again, or against a checkout that already has the
# defect, does nothing and exits cleanly.
#
# Usage (from the repository root):
#   ./exercises/scripts/apply-exercise-03-defect.sh
#   ./exercises/scripts/apply-exercise-03-defect.sh --branch exercise-03 --commit
#
#   (no flags)        edit the two files in the current working tree only.
#   --branch NAME      also create/switch to branch NAME and stage the changes.
#   --commit           with --branch, also commit the staged changes.

set -euo pipefail

SERVICE_FILE="src/main/java/com/atlas/inventory/InventoryService.java"
TEST_FILE="src/test/java/com/atlas/inventory/InventoryServiceTest.java"

BRANCH=""
DO_COMMIT=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --branch) BRANCH="${2:?--branch requires a name}"; shift 2 ;;
    --commit) DO_COMMIT=1; shift ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

if [[ ! -f pom.xml || ! -f "$SERVICE_FILE" || ! -f "$TEST_FILE" ]]; then
  echo "error: run this from the repository root (expected to find pom.xml and $SERVICE_FILE)" >&2
  exit 1
fi

if grep -q "totalReorderShortage" "$SERVICE_FILE"; then
  echo "Already applied: totalReorderShortage already exists in $SERVICE_FILE. Nothing to do."
else
  awk '
    { print }
    /return repository\.isHealthy\(\);/ {
      getline closingBrace
      print closingBrace
      print ""
      print "    public static int totalReorderShortage(List<InventoryItem> items) {"
      print "        int total = 0;"
      print "        for (InventoryItem item : items) {"
      print "            total += item.reorderLevel() - item.quantity();"
      print "        }"
      print "        return total;"
      print "    }"
    }
  ' "$SERVICE_FILE" > "$SERVICE_FILE.tmp"
  mv "$SERVICE_FILE.tmp" "$SERVICE_FILE"
  echo "Inserted totalReorderShortage() into $SERVICE_FILE"
fi

if grep -q "totalShortageAcrossMultipleItemsIgnoresItemsAboveReorderLevel" "$TEST_FILE"; then
  echo "Already applied: the regression test already exists in $TEST_FILE. Nothing to do."
else
  awk '
    /^import java\.nio\.file\.Path;$/ && !addedImport {
      print
      print "import java.util.List;"
      addedImport = 1
      next
    }
    /private static InventoryItem newItem\(String partNumber, int quantity\) \{/ && !addedTest {
      print "    @Test"
      print "    void totalShortageAcrossMultipleItemsIgnoresItemsAboveReorderLevel() {"
      print "        InventoryItem shortItem = new InventoryItem("
      print "                0, \"A-1\", \"Short Item\", \"Component\", \"A-01-01\", 2, 10, \"\");"
      print "        InventoryItem surplusItem = new InventoryItem("
      print "                0, \"B-1\", \"Surplus Item\", \"Component\", \"A-02-01\", 50, 5, \"\");"
      print ""
      print "        int total = InventoryService.totalReorderShortage(List.of(shortItem, surplusItem));"
      print ""
      print "        assertEquals(8, total);"
      print "    }"
      print ""
      addedTest = 1
      print
      next
    }
    { print }
  ' "$TEST_FILE" > "$TEST_FILE.tmp"
  mv "$TEST_FILE.tmp" "$TEST_FILE"
  echo "Inserted the failing regression test into $TEST_FILE"
fi

echo
echo "Confirming the test fails as expected (mvn test -Dtest=InventoryServiceTest)..."
LOG="$(mktemp)"
if mvn --batch-mode -q -Dtest=InventoryServiceTest test > "$LOG" 2>&1; then
  echo "WARNING: mvn test succeeded — the defect does not appear to be in place." >&2
  tail -n 40 "$LOG"
  exit 1
fi
if grep -q "expected: <8> but was: <-37>" "$LOG"; then
  echo "Confirmed: totalShortageAcrossMultipleItemsIgnoresItemsAboveReorderLevel fails with -37 instead of 8, as expected."
else
  echo "WARNING: the test failed, but not with the expected -37 vs 8 mismatch:" >&2
  tail -n 40 "$LOG"
  exit 1
fi

if [[ -n "$BRANCH" ]]; then
  git checkout -B "$BRANCH"
  git add "$SERVICE_FILE" "$TEST_FILE"
  if [[ "$DO_COMMIT" -eq 1 ]]; then
    git commit -m "Add failing totalReorderShortage test for exercise 3"
    echo "Committed on branch '$BRANCH'. Push it with: git push -u origin $BRANCH"
  else
    echo "Staged on branch '$BRANCH'. Commit with: git commit -m \"Add failing totalReorderShortage test for exercise 3\""
  fi
fi
