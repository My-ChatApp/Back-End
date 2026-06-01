# Apply load-test/data/seed.sql to local Postgres Docker container.
param(
    [string]$Container = "mychatapp-postgres",
    [string]$User = "mychatapp",
    [string]$Database = "mychatapp"
)

$ErrorActionPreference = "Stop"
$BackEndRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$SqlPath = Join-Path $BackEndRoot "load-test\data\seed.sql"

if (-not (Test-Path $SqlPath)) {
    throw "seed.sql not found: $SqlPath`nGenerate first: node load-test/scripts/generate-loadtest-data.mjs --count 500 --out-dir load-test/data"
}

Write-Host "Applying $SqlPath to container $Container..." -ForegroundColor Cyan
Get-Content $SqlPath -Raw | docker exec -i $Container psql -U $User -d $Database
Write-Host "Seed applied." -ForegroundColor Green
