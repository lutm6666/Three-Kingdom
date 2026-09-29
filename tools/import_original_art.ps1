param(
    [Parameter(Mandatory = $true)]
    [string]$SourceImagesRoot
)

$ErrorActionPreference = "Stop"
$dest = Join-Path $PSScriptRoot "..\app\src\main\res\drawable-nodpi"
$char = Join-Path $SourceImagesRoot "character\resources-auto"
$layout = Join-Path $SourceImagesRoot "layout\resources-auto"

New-Item -ItemType Directory -Force $dest | Out-Null

foreach ($name in @("bg_download.png", "button_middle.png", "info_window.png")) {
    $src = Join-Path $layout $name
    if (Test-Path $src) {
        Copy-Item $src (Join-Path $dest ("sgpz_" + $name)) -Force
    }
}

Get-ChildItem $char -Filter "*_l.png" | ForEach-Object {
    Copy-Item $_.FullName (Join-Path $dest ("sgpz_" + $_.Name.ToLower())) -Force
}

$count = (Get-ChildItem $dest -Filter "sgpz_*.png").Count
Write-Host "Imported $count local original-art PNG files."
Write-Host "These files are gitignored and remain local."
