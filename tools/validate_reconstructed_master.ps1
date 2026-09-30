param(
    [string]$Path = (Join-Path $PSScriptRoot "..\app\src\main\assets\data\reconstructed_master_v1.json")
)

$ErrorActionPreference = "Stop"
$allowed = @("VERIFIED", "RECONSTRUCTED", "PARTIAL", "UNKNOWN")

function Check-ConfidenceValue([object]$value, [string]$where) {
    if ($null -eq $value) {
        throw "Missing confidence at $where"
    }

    $s = [string]$value
    if ($allowed -notcontains $s) {
        throw "Invalid confidence '$s' at $where"
    }
}

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

$mappedArtCount = 0

foreach ($c in $m.characters) {
    Check-ConfidenceValue $c.confidence "character:$($c.id)"
    Check-ConfidenceValue $c.stats.confidence "character:$($c.id).stats"
    Check-ConfidenceValue $c.active_skill.confidence "character:$($c.id).active_skill"

    if ($null -ne $c.leader_skill) {
        Check-ConfidenceValue $c.leader_skill.confidence "character:$($c.id).leader_skill"
    }

    Check-ConfidenceValue $c.art_identity_confidence "character:$($c.id).art"

    if ($c.confidence -eq "VERIFIED" -and $null -eq $c.card_no) {
        throw "VERIFIED character lacks card_no: $($c.id)"
    }

    if ($null -eq $c.art_asset_id) {
        if ($c.art_identity_confidence -eq "VERIFIED") {
            throw "VERIFIED art confidence without art_asset_id: $($c.id)"
        }
    } else {
        if ($c.art_identity_confidence -ne "VERIFIED") {
            throw "Mapped art must be VERIFIED: $($c.id)"
        }
        $mappedArtCount++
    }
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

    Check-ConfidenceValue $s.field_confidence.difficulty "stage:$($s.id).difficulty"
    Check-ConfidenceValue $s.field_confidence.stamina "stage:$($s.id).stamina"
    Check-ConfidenceValue $s.field_confidence.wave_count "stage:$($s.id).wave_count"
    Check-ConfidenceValue $s.rewards.confidence "stage:$($s.id).rewards"
    Check-ConfidenceValue $s.composition.confidence "stage:$($s.id).composition"
    Check-ConfidenceValue $s.encounter_rule.confidence "stage:$($s.id).encounter_rule"

    foreach ($e in @($s.common_pool) + @($s.boss_wave)) {
        Check-ConfidenceValue $e.confidence.atk "stage:$($s.id).enemy:$($e.name).atk"
        Check-ConfidenceValue $e.confidence.turn "stage:$($s.id).enemy:$($e.name).turn"
        Check-ConfidenceValue $e.confidence.hp_def "stage:$($s.id).enemy:$($e.name).hp_def"
    }
}

Check-ConfidenceValue $m.enums.unit_advantage.confidence "enums.unit_advantage"
Check-ConfidenceValue $m.mechanics.faction_advantage.confidence "mechanics.faction_advantage"
Check-ConfidenceValue $m.mechanics.encounter_seed.confidence "mechanics.encounter_seed"
Check-ConfidenceValue $m.mechanics.drop_rate_percent.confidence "mechanics.drop_rate_percent"
Check-ConfidenceValue $m.mechanics.enhancement.confidence "mechanics.enhancement"
Check-ConfidenceValue $m.mechanics.enhancement.low_rarity_material_base_growth.confidence "mechanics.enhancement.low_rarity_material_base_growth"

if ($m.mechanics.enhancement.confidence -eq "VERIFIED" -and
    $m.mechanics.enhancement.low_rarity_material_base_growth.confidence -ne "VERIFIED") {
    throw "VERIFIED enhancement parent contains non-VERIFIED child"
}

if ([int]$m.art_assets.identities_verified -ne $mappedArtCount) {
    throw "Art identity count mismatch: declared=$($m.art_assets.identities_verified) actual=$mappedArtCount"
}

$verifiedChars = @($m.characters | Where-Object { $_.confidence -eq "VERIFIED" }).Count
$partialChars = @($m.characters | Where-Object { $_.confidence -eq "PARTIAL" }).Count
$reconstructedStages = @($m.stages | Where-Object { $_.encounter_rule.confidence -eq "RECONSTRUCTED" }).Count

Write-Output "MASTER_VALID=TRUE"
Write-Output ("SCHEMA=" + $m.meta.schema)
Write-Output ("VERSION=" + $m.meta.version)
Write-Output ("CHARACTERS=" + $m.characters.Count)
Write-Output ("VERIFIED_CHARACTERS=" + $verifiedChars)
Write-Output ("PARTIAL_CHARACTERS=" + $partialChars)
Write-Output ("STAGES=" + $m.stages.Count)
Write-Output ("RECONSTRUCTED_ENCOUNTER_STAGES=" + $reconstructedStages)
Write-Output ("ART_IDENTITIES_VERIFIED=" + $m.art_assets.identities_verified)
