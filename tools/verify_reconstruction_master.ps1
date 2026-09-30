$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jsonPath = Join-Path $root 'app\src\main\assets\data\master_reconstruction_v1.json'
$data = Get-Content $jsonPath -Raw -Encoding UTF8 | ConvertFrom-Json

if ($data.schema_version -ne 1) { throw 'schema_version mismatch' }
if ($data.generals.Count -ne 10) { throw "expected 10 generals, got $($data.generals.Count)" }

$ids = @($data.generals | ForEach-Object {[int]$_.id})
if (($ids | Sort-Object -Unique).Count -ne 10) { throw 'duplicate general id' }
if (($ids | Measure-Object -Minimum).Minimum -ne 0 -or ($ids | Measure-Object -Maximum).Maximum -ne 9) {
    throw 'general ids must cover 0..9'
}

$valid = @('ORIGINAL_VERIFIED','RECONSTRUCTED','CUSTOM')
foreach ($g in $data.generals) {
    if ($valid -notcontains $g.provenance) { throw "bad provenance for $($g.name)" }
    if ($g.max_level -le 0) { throw "bad max_level for $($g.name)" }
    if ($g.lv1.hp -le 0 -or $g.lv1.atk -le 0) { throw "bad lv1 stats for $($g.name)" }
    if ($g.lvmax.hp -lt $g.lv1.hp -or $g.lvmax.atk -lt $g.lv1.atk) {
        throw "max stats below lv1 for $($g.name)"
    }
}

$verified = @($data.generals | Where-Object {$_.provenance -eq 'ORIGINAL_VERIFIED'}).Count
$reconstructed = @($data.generals | Where-Object {$_.provenance -eq 'RECONSTRUCTED'}).Count
$custom = @($data.generals | Where-Object {$_.provenance -eq 'CUSTOM'}).Count

Write-Output "SCHEMA=$($data.schema_version)"
Write-Output "GENERALS=$($data.generals.Count)"
Write-Output "ORIGINAL_VERIFIED=$verified"
Write-Output "RECONSTRUCTED=$reconstructed"
Write-Output "CUSTOM=$custom"
Write-Output 'VALIDATION=PASS'
