# Kiem tra khoi dong tung service (timeout moi service)
$ErrorActionPreference = "Continue"
$Root = Split-Path $PSScriptRoot -Parent
$services = @(
    @{ Name = "auth-service";         Dir = "auth-service";         Port = 8081; Pattern = "Started AuthServiceApplication" },
    @{ Name = "chat-service";         Dir = "chat-service";         Port = 8082; Pattern = "Started ChatServiceApplication" },
    @{ Name = "user-service";         Dir = "user-service";         Port = 8083; Pattern = "Started UserServiceApplication" },
    @{ Name = "notification-service"; Dir = "notification-service"; Port = 8084; Pattern = "Started NotificationServiceApplication" },
    @{ Name = "media-service";        Dir = "media-service";        Port = 8085; Pattern = "media-service listening"; Runtime = "node" },
    @{ Name = "api-gateway";          Dir = "api-gateway";          Port = 8080; Pattern = "Started ApiGatewayApplication" }
)

function Stop-TestJava {
    Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

$report = @()
foreach ($svc in $services) {
    Stop-TestJava
    $wd = Join-Path $Root $svc.Dir
    $mvnw = if (Test-Path "$wd\mvnw.cmd") { "$wd\mvnw.cmd" } else { Join-Path $Root "mvnw.cmd" }
    $log = Join-Path $env:TEMP "mychatapp-startup-$($svc.Name).log"
    Remove-Item $log -ErrorAction SilentlyContinue

    if ($svc.Runtime -eq "node") {
        if (-not (Test-Path "$wd\node_modules")) {
            & npm install --prefix $wd 2>&1 | Out-Null
        }
        $startCmd = "cd /d `"$wd`" && npm run dev > `"$log`" 2>&1"
    } else {
        $startCmd = "cd /d `"$wd`" && `"$mvnw`" spring-boot:run -q > `"$log`" 2>&1"
    }
    $proc = Start-Process -FilePath "cmd.exe" -ArgumentList @(
        "/c", $startCmd
    ) -PassThru -WindowStyle Hidden

    $ok = $false
    $deadline = (Get-Date).AddSeconds(100)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 2
        if (-not (Test-Path $log)) { continue }
        $text = Get-Content $log -Raw -ErrorAction SilentlyContinue
        if ($text -match [regex]::Escape($svc.Pattern)) { $ok = $true; break }
        if ($text -match "BUILD FAILURE|Application run failed|Error:|ECONNREFUSED") { break }
        if ($proc.HasExited) { break }
    }

    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    Stop-TestJava

    $tail = @()
    $rootCause = ""
    if (Test-Path $log) {
        $lines = Get-Content $log
        $tail = $lines | Select-Object -Last 12
        $rootCause = ($lines | Select-String -Pattern "SchemaManagementException|Application run failed|BUILD FAILURE|FATAL:|Caused by: org" | Select-Object -Last 3 | ForEach-Object { $_.Line.Trim() }) -join " || "
    }

    $report += [PSCustomObject]@{
        Service   = $svc.Name
        Port      = $svc.Port
        Started   = $ok
        RootCause = $rootCause
    }
    Write-Host ("[{0}] {1} port {2}" -f $(if ($ok) { "OK" } else { "FAIL" }), $svc.Name, $svc.Port) -ForegroundColor $(if ($ok) { "Green" } else { "Red" })
    if (-not $ok -and $rootCause) { Write-Host "  $rootCause" -ForegroundColor Yellow }
}

Write-Host "`n=== TOM TAT ===" -ForegroundColor Cyan
$report | Format-Table -AutoSize
$failed = $report | Where-Object { -not $_.Started }
if ($failed) { exit 1 }
exit 0
