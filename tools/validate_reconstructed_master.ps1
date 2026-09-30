param(
    [string]$Path = (Join-Path $PSScriptRoot "..\app\src\main\assets\data\reconstructed_master_v1.json")
)

$ErrorActionPreference = "Stop"
$allowed = @("VERIFIED", "RECONSTRUCTED", "PARTIAL", "UNKNOWN")

if (-not (Test-Path $Path)) {
    throw "Master file not found: $Path"
}

$m = Get-Content $Path -Raw -Encoding UTF8 | ConvertFrom-Json

if ($m.meta.schema -ne "sgpz-reconstructed-master") {
    throw "Unexpected schema: $($m.meta.schema)"
}
if ([int]$m.meta.version -ne 1) {
    throw "Unexpected master version: $($m.meta.version)"
}
if ($m.characters.Count -lt 1) {
    throw "No characters"
}
if ($m.stages.Count -lt 1) {
    throw "No stages"
}

$charIds = @($m.characters | ForEach-Object { $_.id })
if (($charIds | Sort-Object -Unique).Count -ne $charIds.Count) {
    throw "Duplicate character id"
}

$stageIds = @($m.stages | ForEach-Object { $_.id })
if (($stageIds | Sort-Object -Unique).Count -ne $stageIds.Count) {
    throw "Duplicate stage id"
}

function Check-ConfidenceValue([object]$value, [string]$where) {
    if ($null -eq $value) { return }
    $s = [string]$value
    if ($allowed -notcontains $s) {
        throw "Invalid confidence '$s' at $where"
    }
}

foreach ($c in $m.characters) {
    Check-ConfidenceValue $c.confidence "character:$($c.id)"
    Check-ConfidenceValue $c.stats.confidence "character:$($c.id).stats"
    Check-ConfidenceValue $c.active_skill.confidence "character:$($c.id).active_skill"
    if ($null -ne $c.leader_skill) {
        Check-ConfidenceValue $c.leader_skill.confidence "character:$($c.id).leader_skill"
    }
    Check-ConfidenceValue $c.art_identity_confidence "character:$($c.id).art"
}

foreach ($s in $m.stages) {
    if ([int]$s.wave_count -lt 1) {
        throw "Invalid wave_count at stage:$($s.id)"
    }
    if ($s.common_pool.Count -lt 1) {
        throw "Empty common_pool at stage:$($s.id)"
    }
    if ($s.boss_wave.Count -lt 1) {
        throw "Empty boss_wave at stage:$($s.id)"
    }
    Check-ConfidenceValue $s.rewards.confidence "stage:$($s.id).rewards"
    Check-ConfidenceValue $s.composition.confidence "stage:$($s.id).composition"
    Check-ConfidenceValue $s.encounter_rule.confidence "stage:$($s.id).encounter_rule"

    foreach ($e in @($s.common_pool) + @($s.boss_wave)) {
        Check-ConfidenceValue $e.confidence.atk "stage:$($s.id).enemy:$($e.name).atk"
        Check-ConfidenceValue $e.confidence.turn "stage:$($s.id).enemy:$($e.name).turn"
        Check-ConfidenceValue $e.confidence.hp_def "stage:$($s.id).enemy:$($e.name).hp_def"
    }
}

if ([int]$m.art_assets.identities_verified -ne 0) {
    throw "Art identity count must remain 0 until non-circular evidence exists"
}

$verifiedChars = @($m.characters | Where-Object { $_.confidence -eq "VERIFIED" }).Count
$reconstructedStages = @($m.stages | Where-Object { $_.encounter_rule.confidence -eq "RECONSTRUCTED" }).Count

Write-Output "MASTER_VALID=TRUE"
Write-Output ("SCHEMA=" + $m.meta.schema)
Write-Output ("VERSION=" + $m.meta.version)
Write-Output ("CHARACTERS=" + $m.characters.Count)
Write-Output ("VERIFIED_CHARACTERS=" + $verifiedChars)
Write-Output ("STAGES=" + $m.stages.Count)
Write-Output ("RECONSTRUCTED_ENCOUNTER_STAGES=" + $reconstructedStages)
Write-Output ("ART_IDENTITIES_VERIFIED=" + $m.art_assets.identities_verified)
