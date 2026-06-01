# WebSocket load: each user sends N messages via STOMP, then GET messages.
# Defaults match "500 users concurrent" profile (ramp 180s, concurrency 500).
#
# Examples:
#   .\load-test\scripts\run-ws-load.ps1 -Users 10 -Concurrency 10 -RampUpSec 10 -Messages 5
#   .\load-test\scripts\run-ws-load.ps1 -Users 500 -Concurrency 500 -RampUpSec 180 -Messages 5

param(
    [string]$HostName = "localhost",
    [int]$Port = 8080,
    [int]$Users = 500,
    [int]$Concurrency = 500,
    [int]$RampUpSec = 180,
    [int]$Messages = 5,
    [int]$SettleMs = 2000,
    [int]$SendGapMs = 0,
    [int]$ServerWaitMs = 8000,
    [switch]$Debug
)

$ErrorActionPreference = "Stop"
$BackEndRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$LoadTestRoot = Join-Path $BackEndRoot "load-test"
$Script = Join-Path $LoadTestRoot "scripts\ws-send-then-get.mjs"
$Csv = Join-Path $LoadTestRoot "data\users.csv"
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$OutDir = Join-Path $LoadTestRoot "results\ws-$Timestamp"

if (-not (Test-Path $Script)) {
    throw "Script not found: $Script"
}
if (-not (Test-Path $Csv)) {
    throw "users.csv not found: $Csv`nRun generate-loadtest-data.mjs and apply-seed.ps1 first."
}

$nodeArgs = @(
    $Script,
    "--host", $HostName,
    "--port", $Port,
    "--csv", $Csv,
    "--users", $Users,
    "--concurrency", $Concurrency,
    "--ramp-up-sec", $RampUpSec,
    "--messages", $Messages,
    "--settle-ms", $SettleMs,
    "--send-gap-ms", $SendGapMs,
    "--server-wait-ms", $ServerWaitMs,
    "--out-dir", $OutDir
)
if ($Debug) { $nodeArgs += "--debug" }

Write-Host "Installing load-test npm deps if needed..." -ForegroundColor DarkGray
Push-Location $LoadTestRoot
npm install --omit=dev 2>&1 | Out-Null
Pop-Location

& node @nodeArgs
$code = $LASTEXITCODE
if ($code -ne 0) { exit $code }

Write-Host ""
Write-Host "Results:       $OutDir" -ForegroundColor Green
Write-Host "HTML report:   $OutDir\html-report\index.html" -ForegroundColor Green
