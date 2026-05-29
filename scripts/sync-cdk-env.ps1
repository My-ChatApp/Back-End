# Cap nhat .env tu CloudFormation outputs sau CDK deploy
# Usage:
#   .\scripts\sync-cdk-env.ps1
#   .\scripts\sync-cdk-env.ps1 -StackName MyChatApp-dev

param(
    [string]$StackName = "MyChatApp-dev"
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path $PSScriptRoot -Parent
$EnvFile = Join-Path $RootDir ".env"

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    throw "Can cai AWS CLI (aws) de doc stack outputs."
}

if (-not (Test-Path $EnvFile)) {
    Copy-Item (Join-Path $RootDir ".env.example") $EnvFile
    Write-Host "Da tao .env tu .env.example" -ForegroundColor Yellow
}

Write-Host "Doc outputs tu stack: $StackName" -ForegroundColor Cyan

$raw = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --query "Stacks[0].Outputs" `
    --output json 2>&1

if ($LASTEXITCODE -ne 0) {
    throw "Khong doc duoc stack '$StackName'. Chay CDK deploy truoc: cd infra\cdk; npm run deploy"
}

$outputs = $raw | ConvertFrom-Json
$map = @{}
foreach ($o in $outputs) {
    $map[$o.OutputKey] = $o.OutputValue
}

$bucket = $map["AttachmentsBucketName"]
$mediaUrl = $map["MediaPublicBaseUrl"]

if (-not $bucket -and -not $mediaUrl) {
    throw "Stack khong co output AttachmentsBucketName / MediaPublicBaseUrl"
}

function Set-EnvLine {
    param([string]$Key, [string]$Value)
    if (-not $Value) { return }
    $content = Get-Content $EnvFile -Raw
    $pattern = "(?m)^$Key=.*$"
    $line = "$Key=$Value"
    if ($content -match $pattern) {
        $content = [regex]::Replace($content, $pattern, $line)
    } else {
        if (-not $content.EndsWith("`n")) { $content += "`n" }
        $content += "$line`n"
    }
    Set-Content -Path $EnvFile -Value $content.TrimEnd() -NoNewline
    Write-Host "  $Key=$Value" -ForegroundColor Green
}

function Remove-EnvLine {
    param([string]$Key)
    $content = Get-Content $EnvFile -Raw
    $pattern = "(?m)^$Key=.*(`r?`n)?"
    if ($content -notmatch $pattern) { return }
    $content = [regex]::Replace($content, $pattern, "")
    Set-Content -Path $EnvFile -Value $content.TrimEnd() -NoNewline
    Write-Host "  (xoa) $Key — dung AWS DynamoDB" -ForegroundColor DarkYellow
}

Write-Host "Cap nhat $EnvFile" -ForegroundColor Cyan
if ($bucket) { Set-EnvLine -Key "S3_BUCKET" -Value $bucket }
if ($mediaUrl) { Set-EnvLine -Key "MEDIA_PUBLIC_BASE_URL" -Value $mediaUrl.TrimEnd('/') }

$dynamoTable = $map["DynamoDbTableName"]
if ($dynamoTable) {
    Set-EnvLine -Key "DYNAMODB_TABLE" -Value $dynamoTable
    Remove-EnvLine -Key "DYNAMODB_ENDPOINT"
}

# Region stack CDK (ap-southeast-1 trong stack hien tai)
Set-EnvLine -Key "AWS_REGION" -Value "ap-southeast-1"

$valkeyHost = $map["ValkeyPrimaryEndpoint"]
if ($valkeyHost) {
    Set-EnvLine -Key "SPRING_DATA_REDIS_HOST" -Value $valkeyHost
    Set-EnvLine -Key "SPRING_DATA_REDIS_SSL_ENABLED" -Value "true"
    Write-Host "  Valkey endpoint synced (xem infra/cdk/VALKEY.md cho AUTH token)" -ForegroundColor Green
}

Write-Host "Hoan tat sync CDK -> .env" -ForegroundColor Green
