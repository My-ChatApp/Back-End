# Dừng stack Docker local
param(
    [switch] $AppsOnly
)

$ErrorActionPreference = 'Stop'
$RootDir = Split-Path $PSScriptRoot -Parent
Set-Location $RootDir

$composeArgs = @(
    '-f', 'docker-compose.yml',
    '-f', 'docker-compose.apps.yml'
)

if ($AppsOnly) {
    $appServices = @(
        'api-gateway', 'auth-service', 'chat-service', 'user-service',
        'notification-service', 'media-service', 'magika', 'agent-service'
    )
    docker compose @composeArgs stop @appServices 2>$null
    docker compose @composeArgs rm -f @appServices 2>$null
    Write-Host "App containers stopped. Infra (postgres, rabbitmq, valkey) still running." -ForegroundColor Green
} else {
    docker compose @composeArgs down
    docker compose -f docker-compose.yml down
    Write-Host "All containers stopped." -ForegroundColor Green
}
