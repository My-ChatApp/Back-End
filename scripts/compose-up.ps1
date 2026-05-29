# Infra (postgres, rabbitmq, valkey) + app images đã build local
# Usage (from Back-End-NDuy):
#   .\scripts\docker-build.ps1 -Service all -Tag v1
#   copy .env.docker.example .env.docker
#   .\scripts\compose-up.ps1

param(
    [string] $Tag = ''
)

$ErrorActionPreference = 'Stop'

$RootDir = Split-Path $PSScriptRoot -Parent
$EnvDocker = Join-Path $RootDir '.env.docker'
$EnvExample = Join-Path $RootDir '.env.docker.example'

if (-not (Test-Path $EnvDocker)) {
    $legacy = Join-Path $RootDir 'DockerTest\.env.docker'
    if (Test-Path $legacy) {
        Copy-Item $legacy $EnvDocker
        Write-Host "Copied DockerTest\.env.docker -> .env.docker" -ForegroundColor Yellow
    } else {
        Copy-Item $EnvExample $EnvDocker
        Write-Host "Created $EnvDocker — chỉnh AWS/S3/JWT (có thể copy từ .env)." -ForegroundColor Yellow
    }
}

Set-Location $RootDir

if (-not $Tag -and (Test-Path $EnvDocker)) {
    foreach ($line in Get-Content $EnvDocker) {
        if ($line -match '^\s*IMAGE_TAG\s*=\s*(.+)\s*$') {
            $Tag = $Matches[1].Trim()
            break
        }
    }
}
if (-not $Tag) { $Tag = 'v1' }

$requiredImages = @(
    "mychatapp:gateway-$Tag",
    "mychatapp:auth-$Tag",
    "mychatapp:chat-$Tag",
    "mychatapp:user-$Tag",
    "mychatapp:notification-$Tag",
    "mychatapp:media-$Tag",
    "mychatapp:magika-$Tag"
)

Write-Host "Checking local images (tag: $Tag)..." -ForegroundColor Cyan
$missing = @()
foreach ($img in $requiredImages) {
    docker image inspect $img 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) { $missing += $img }
}
if ($missing.Count -gt 0) {
    Write-Host "Missing images:" -ForegroundColor Red
    $missing | ForEach-Object { Write-Host "  - $_" }
    throw "Build first: .\scripts\docker-build.ps1 -Service all -Tag $Tag"
}

Write-Host "Starting infra (postgres, rabbitmq, valkey)..." -ForegroundColor Cyan
docker compose -f docker-compose.yml up -d postgres rabbitmq valkey
if ($LASTEXITCODE -ne 0) { throw 'docker compose infra failed' }

Write-Host "Starting app containers..." -ForegroundColor Cyan
docker compose `
    --env-file $EnvDocker `
    -f docker-compose.yml `
    -f docker-compose.apps.yml `
    up -d

if ($LASTEXITCODE -ne 0) { throw 'docker compose apps failed' }

Write-Host @"

Ready:
  Gateway:  http://localhost:8080/api/auth/health
  Auth:     http://localhost:8081/api/auth/health
  Magika:   http://localhost:8090/health
  RabbitMQ: http://localhost:15673  (guest/guest)
  Postgres: localhost:5433 (user mychatapp)

Stop apps:  .\scripts\compose-down.ps1 -AppsOnly
Stop all:   .\scripts\compose-down.ps1

"@ -ForegroundColor Green
