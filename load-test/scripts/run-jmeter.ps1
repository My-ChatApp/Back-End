# Run JMeter load test (non-GUI) against api-gateway.
# Requires Apache JMeter on PATH (jmeter.bat) or JMETER_HOME set.
#
# Example:
#   .\load-test\scripts\run-jmeter.ps1 -Threads 10 -RampUp 10 -Loops 2
#   .\load-test\scripts\run-jmeter.ps1 -Threads 500 -RampUp 180 -Loops 20

param(
    [string]$HostName = "localhost",
    [int]$Port = 8080,
    [int]$Threads = 500,
    [int]$RampUp = 180,
    [int]$Loops = 20,
    [int]$ThinkMin = 1000,
    [int]$ThinkMax = 3000,
    [string]$JmeterHome = ""
)

$ErrorActionPreference = "Stop"
$BackEndRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$LoadTestRoot = Join-Path $BackEndRoot "load-test"
$Jmx = Join-Path $LoadTestRoot "jmeter\MyChatApp-500users-http.jmx"
$Csv = Join-Path $LoadTestRoot "data\users.csv"
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$ResultDir = Join-Path $LoadTestRoot "results\$Timestamp"
$Jtl = Join-Path $ResultDir "results.jtl"
$HtmlReport = Join-Path $ResultDir "html-report"
$JmeterLog = Join-Path $ResultDir "jmeter.log"

if (-not (Test-Path $Jmx)) {
    throw "JMX not found: $Jmx"
}
if (-not (Test-Path $Csv)) {
    throw "users.csv not found: $Csv`nRun: node load-test/scripts/generate-loadtest-data.mjs --count 500 --out-dir load-test/data`nThen apply seed.sql to Postgres."
}

. (Join-Path $PSScriptRoot "Resolve-JMeter.ps1")
$jmeterCmd = Resolve-JMeterBat -ExplicitHome $JmeterHome
if (-not $jmeterCmd) {
    throw (Get-JMeterInstallHint)
}

$thinkRange = [Math]::Max(0, $ThinkMax - $ThinkMin)
New-Item -ItemType Directory -Path $ResultDir -Force | Out-Null

Write-Host "MyChatApp JMeter load test" -ForegroundColor Cyan
Write-Host "  Target:    http://${HostName}:${Port}"
Write-Host "  Threads:   $Threads  Ramp-up: ${RampUp}s  Loops: $Loops"
Write-Host "  CSV:       $Csv"
Write-Host "  Results:   $ResultDir"
Write-Host ""

$env:HEAP = "-Xms1g -Xmx4g"

# Quote all -J values: project path may contain spaces (e.g. "2025-2026 HK2").
$runArgs = @(
    "-n",
    "-t", $Jmx,
    "-l", $Jtl,
    "-j", $JmeterLog,
    "-Jhost=$HostName",
    "-Jport=$Port",
    "-Jthreads=$Threads",
    "-Jrampup=$RampUp",
    "-Jloops=$Loops",
    "-Jthink_min=$ThinkMin",
    "-Jthink_range=$thinkRange",
    "-Jcsvfile=$Csv"
)

& $jmeterCmd @runArgs

if ($LASTEXITCODE -ne 0) {
    throw "JMeter test run exited with code $LASTEXITCODE (see $JmeterLog)"
}

$lineCount = 0
if (Test-Path $Jtl) {
    $lineCount = (Get-Content $Jtl | Measure-Object -Line).Lines
}

if ($lineCount -le 1) {
    Write-Host ""
    Write-Warning "No samples recorded (JTL empty). Check gateway at http://${HostName}:${Port} and jmeter.log."
    Write-Host "  Log: $JmeterLog"
    exit 1
}

Write-Host ""
Write-Host "Generating HTML report..." -ForegroundColor Cyan
$reportArgs = @("-g", $Jtl, "-o", $HtmlReport)
& $jmeterCmd @reportArgs

if ($LASTEXITCODE -ne 0) {
    Write-Warning "HTML report generation failed (JTL still valid at $Jtl)"
} else {
    Write-Host ""
    Write-Host "Done." -ForegroundColor Green
    Write-Host "  JTL:         $Jtl"
    Write-Host "  HTML report: $HtmlReport\index.html"
}
