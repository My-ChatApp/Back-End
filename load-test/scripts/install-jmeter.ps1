# Install Apache JMeter via winget (Windows).
# After install, open a NEW terminal or set JMETER_HOME before run-jmeter.ps1.

$ErrorActionPreference = "Stop"

Write-Host "Installing Apache JMeter (DEVCOM.JMeter) via winget..." -ForegroundColor Cyan

$winget = Get-Command winget -ErrorAction SilentlyContinue
if (-not $winget) {
    throw "winget not found. Install App Installer from Microsoft Store, or download JMeter manually from https://jmeter.apache.org/download_jmeter.cgi"
}

& winget install --id DEVCOM.JMeter -e `
    --accept-package-agreements `
    --accept-source-agreements

if ($LASTEXITCODE -ne 0) {
    throw "winget install failed with exit code $LASTEXITCODE"
}

. (Join-Path $PSScriptRoot "Resolve-JMeter.ps1")
$bat = Resolve-JMeterBat
if ($bat) {
    $home = Split-Path (Split-Path $bat -Parent) -Parent
    Write-Host ""
    Write-Host "JMeter found at: $bat" -ForegroundColor Green
    Write-Host "Set for current session:" -ForegroundColor Yellow
    Write-Host "  `$env:JMETER_HOME = `"$home`""
    Write-Host ""
    Write-Host "Then run smoke test:" -ForegroundColor Yellow
    Write-Host "  .\load-test\scripts\run-jmeter.ps1 -JmeterHome `"$home`" -Threads 10 -RampUp 10 -Loops 2"
} else {
    Write-Host ""
    Write-Host "Install finished. Open a NEW PowerShell window, then run run-jmeter.ps1." -ForegroundColor Yellow
    Write-Host "If still not found, download zip from https://jmeter.apache.org/download_jmeter.cgi" -ForegroundColor Yellow
    Write-Host "  and pass: -JmeterHome `"C:\path\to\apache-jmeter-5.6.3`"" -ForegroundColor Yellow
}
