# Deploy CDK (S3 + CloudFront + DynamoDB + SES) va cap nhat .env
# Usage:
#   .\scripts\deploy-infra.ps1
#   .\scripts\deploy-infra.ps1 -SkipEnvSync

param(
    [switch]$SkipEnvSync
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path $PSScriptRoot -Parent
$CdkDir = Join-Path $RootDir "infra\cdk"

Set-Location $CdkDir

if (-not (Test-Path "node_modules")) {
    Write-Host "Cai npm dependencies..." -ForegroundColor Cyan
    npm install
    if ($LASTEXITCODE -ne 0) { throw "npm install that bai" }
}

Write-Host "CDK deploy..." -ForegroundColor Cyan
npm run deploy
if ($LASTEXITCODE -ne 0) { throw "cdk deploy that bai" }

Set-Location $RootDir

if (-not $SkipEnvSync) {
    & (Join-Path $PSScriptRoot "sync-cdk-env.ps1")
}

Write-Host ""
Write-Host "Infra da deploy. Kiem tra .env: S3_BUCKET, MEDIA_PUBLIC_BASE_URL, DYNAMODB_TABLE" -ForegroundColor Yellow
Write-Host "Upload avatar mac dinh (mot lan): aws s3 cp <file.jpg> s3://<bucket>/static/default-avatar.jpg" -ForegroundColor DarkGray
