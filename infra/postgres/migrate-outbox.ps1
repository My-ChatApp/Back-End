# Adds app.outbox_events on an existing Postgres volume.
# Docker init scripts in init/ only run when the data volume is first created.

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptDir "init\04-outbox-events.sql"

if (-not (docker ps --format "{{.Names}}" | Select-String -Pattern "^mychatapp-postgres$" -Quiet)) {
    Write-Error "Container mychatapp-postgres is not running. Start it with: docker compose up -d postgres"
}

Get-Content $sqlFile -Raw | docker exec -i mychatapp-postgres psql -U mychatapp -d mychatapp
Write-Host "Migration applied: 04-outbox-events.sql"
