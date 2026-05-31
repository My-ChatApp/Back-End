# Dung backend dev (Windows PowerShell)
# Usage:
#   .\scripts\stop-dev.ps1
#   .\scripts\stop-dev.ps1 -StopDocker

param(
    [switch]$StopDocker
)

$ErrorActionPreference = "SilentlyContinue"
$RootDir = Split-Path $PSScriptRoot -Parent
Set-Location $RootDir

$currentPid = $PID
$devPorts = @(8080, 8081, 8082, 8083, 8084, 8085, 8088, 8090)
$serviceProcessNames = @('java', 'node', 'python')

Write-Host "Dang dung cac service tren port: $($devPorts -join ', ')" -ForegroundColor Cyan

$stopped = 0
foreach ($port in $devPorts) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($conn in $connections) {
        $processId = $conn.OwningProcess
        if (-not $processId -or $processId -le 0) { continue }
        if ($processId -eq $currentPid) {
            Write-Host "  Bo qua port $port — trung shell dang chay stop-dev (PID $currentPid)" -ForegroundColor Yellow
            continue
        }

        $proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if (-not $proc) { continue }

        if ($serviceProcessNames -notcontains $proc.ProcessName) {
            Write-Host "  Bo qua port $port -> PID $processId ($($proc.ProcessName)) — khong phai java/node" -ForegroundColor DarkGray
            continue
        }

        Write-Host "  Stop port $port -> PID $processId ($($proc.ProcessName))" -ForegroundColor DarkGray
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
        $stopped++
    }
}

# Dong cua so PowerShell mo boi run-dev.ps1 (title: "MyChatApp - auth-service", ...)
Get-Process -Name powershell, pwsh -ErrorAction SilentlyContinue | ForEach-Object {
    if ($_.Id -eq $currentPid) { return }
    if ($_.MainWindowTitle -like 'MyChatApp - *') {
        Write-Host "  Dong cua so: $($_.MainWindowTitle)" -ForegroundColor DarkGray
        Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
        $stopped++
    }
}

$magikaContainer = "mychatapp-magika-dev"
$magikaInspect = docker container inspect $magikaContainer 2>$null
if ($LASTEXITCODE -eq 0) {
    docker rm -f $magikaContainer 2>$null | Out-Null
    Write-Host "  Da dung container: $magikaContainer" -ForegroundColor DarkGray
    $stopped++
}

if ($StopDocker) {
    Write-Host "Dang dung Docker infra (postgres, rabbitmq, valkey)..." -ForegroundColor Cyan
    docker compose -f docker-compose.yml down
}

Write-Host ""
if ($stopped -gt 0) {
    Write-Host "Da dung $stopped tien trinh / cua so dev." -ForegroundColor Green
} else {
    Write-Host "Khong tim thay service nao dang lang nghe tren cac port dev." -ForegroundColor Yellow
}

if (-not $StopDocker) {
    Write-Host "Docker infra van chay. Them -StopDocker de tat postgres/rabbitmq/valkey." -ForegroundColor DarkGray
}
