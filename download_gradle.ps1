$ErrorActionPreference = 'Stop'
$url = 'https://services.gradle.org/distributions/gradle-9.6.0-bin.zip'
$dir = 'C:\Users\User\Documents\ThreeKingdomsRebuild\.bootstrap'
$total = 140681093
$parts = 8
$chunk = [math]::Ceiling($total / $parts)
$procs = @()
for ($i = 0; $i -lt $parts; $i++) {
  $start = $i * $chunk
  $end = [math]::Min($total - 1, (($i + 1) * $chunk) - 1)
  $out = Join-Path $dir ("gradle.part{0}" -f $i)
  $args = @('-L','--fail','--silent','--show-error','--range',"$start-$end",$url,'-o',$out)
  $procs += Start-Process -FilePath 'curl.exe' -ArgumentList $args -PassThru
}
$procs | ForEach-Object { $_.WaitForExit(); if ($_.ExitCode -ne 0) { throw "curl failed: $($_.ExitCode)" } }
$outZip = Join-Path $dir 'gradle-9.6.0-bin.zip'
$dest = [System.IO.File]::Open($outZip,[System.IO.FileMode]::Create)
try {
  for ($i = 0; $i -lt $parts; $i++) {
    $src = [System.IO.File]::OpenRead((Join-Path $dir ("gradle.part{0}" -f $i)))
    try { $src.CopyTo($dest) } finally { $src.Dispose() }
  }
} finally { $dest.Dispose() }
Write-Output (Get-Item $outZip | Select-Object FullName,Length | Format-List | Out-String)
