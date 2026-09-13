<#
.SYNOPSIS
  Applies the exercise-03 defect (see exercises/exercise_03_preparation.md) to
  this checkout: a buggy totalReorderShortage() method in InventoryService.java,
  plus its failing regression test in InventoryServiceTest.java.

  Idempotent - running it again, or against a checkout that already has the
  defect, does nothing and exits cleanly.

.PARAMETER Branch
  Also create/switch to this branch and stage the changes.

.PARAMETER Commit
  With -Branch, also commit the staged changes.

.EXAMPLE
  ./exercises/scripts/apply-exercise-03-defect.ps1

.EXAMPLE
  ./exercises/scripts/apply-exercise-03-defect.ps1 -Branch exercise-03 -Commit
#>
param(
    [string]$Branch,
    [switch]$Commit
)

$ErrorActionPreference = "Stop"

$ServiceFile = "src/main/java/com/atlas/inventory/InventoryService.java"
$TestFile = "src/test/java/com/atlas/inventory/InventoryServiceTest.java"

if (-not (Test-Path "pom.xml") -or -not (Test-Path $ServiceFile) -or -not (Test-Path $TestFile)) {
    Write-Error "run this from the repository root (expected to find pom.xml and $ServiceFile)"
    exit 1
}

$serviceLines = Get-Content $ServiceFile
if ($serviceLines -match "totalReorderShortage") {
    Write-Host "Already applied: totalReorderShortage already exists in $ServiceFile. Nothing to do."
} else {
    # Walk by index so we can insert right after the isHealthy() block (the
    # line with "return repository.isHealthy();" plus its closing brace).
    $out = New-Object System.Collections.Generic.List[string]
    for ($i = 0; $i -lt $serviceLines.Count; $i++) {
        $out.Add($serviceLines[$i])
        if ($serviceLines[$i] -match "return repository\.isHealthy\(\);") {
            $i++
            $out.Add($serviceLines[$i])  # the closing "    }"
            $out.Add("")
            $out.Add("    public static int totalReorderShortage(List<InventoryItem> items) {")
            $out.Add("        int total = 0;")
            $out.Add("        for (InventoryItem item : items) {")
            $out.Add("            total += item.reorderLevel() - item.quantity();")
            $out.Add("        }")
            $out.Add("        return total;")
            $out.Add("    }")
        }
    }
    Set-Content -Path $ServiceFile -Value $out
    Write-Host "Inserted totalReorderShortage() into $ServiceFile"
}

$testLines = Get-Content $TestFile
if ($testLines -match "totalShortageAcrossMultipleItemsIgnoresItemsAboveReorderLevel") {
    Write-Host "Already applied: the regression test already exists in $TestFile. Nothing to do."
} else {
    $out = New-Object System.Collections.Generic.List[string]
    foreach ($line in $testLines) {
        if ($line -eq "import java.nio.file.Path;") {
            $out.Add($line)
            $out.Add("import java.util.List;")
            continue
        }
        if ($line -match "private static InventoryItem newItem\(String partNumber, int quantity\) \{") {
            $out.Add("    @Test")
            $out.Add("    void totalShortageAcrossMultipleItemsIgnoresItemsAboveReorderLevel() {")
            $out.Add('        InventoryItem shortItem = new InventoryItem(')
            $out.Add('                0, "A-1", "Short Item", "Component", "A-01-01", 2, 10, "");')
            $out.Add('        InventoryItem surplusItem = new InventoryItem(')
            $out.Add('                0, "B-1", "Surplus Item", "Component", "A-02-01", 50, 5, "");')
            $out.Add("")
            $out.Add("        int total = InventoryService.totalReorderShortage(List.of(shortItem, surplusItem));")
            $out.Add("")
            $out.Add("        assertEquals(8, total);")
            $out.Add("    }")
            $out.Add("")
            $out.Add($line)
            continue
        }
        $out.Add($line)
    }
    Set-Content -Path $TestFile -Value $out
    Write-Host "Inserted the failing regression test into $TestFile"
}

Write-Host ""
Write-Host "Confirming the test fails as expected (mvn test -Dtest=InventoryServiceTest)..."
$log = & mvn --batch-mode -q -Dtest=InventoryServiceTest test 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Warning "mvn test succeeded - the defect does not appear to be in place."
    $log | Select-Object -Last 40
    exit 1
}
if ($log -join "`n" -match "expected: <8> but was: <-37>") {
    Write-Host "Confirmed: totalShortageAcrossMultipleItemsIgnoresItemsAboveReorderLevel fails with -37 instead of 8, as expected."
} else {
    Write-Warning "the test failed, but not with the expected -37 vs 8 mismatch:"
    $log | Select-Object -Last 40
    exit 1
}

if ($Branch) {
    git checkout -B $Branch
    git add $ServiceFile $TestFile
    if ($Commit) {
        git commit -m "Add failing totalReorderShortage test for exercise 3"
        Write-Host "Committed on branch '$Branch'. Push it with: git push -u origin $Branch"
    } else {
        Write-Host "Staged on branch '$Branch'. Commit with: git commit -m `"Add failing totalReorderShortage test for exercise 3`""
    }
}
