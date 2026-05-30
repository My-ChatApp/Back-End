# Build container images for MyChatApp backend (ECS / ECR).
# Usage (from Back-End-NDuy):
#   .\scripts\docker-build.ps1 -Service auth -Tag v1
#   .\scripts\docker-build.ps1 -Service all -Tag v1 -EcrUri 123456789.dkr.ecr.ap-southeast-1.amazonaws.com/mychat-app
#
# EcrUri = repository only (no :latest). Script pushes tags like gateway-v1, auth-v1, ...

param(
    [Parameter()]
    [ValidateSet('all', 'gateway', 'auth', 'chat', 'user', 'notification', 'media', 'magika')]
    [string] $Service = 'all',

    [Parameter()]
    [string] $Tag = 'local',

    [Parameter(Position = 2)]
    [string] $EcrUri = '',

    [Parameter()]
    [string] $ImagePrefix = 'mychatapp'
)

$ErrorActionPreference = 'Stop'

function ConvertTo-EcrRepositoryUri {
    param([string] $Uri)
    if ([string]::IsNullOrWhiteSpace($Uri)) { return '' }
    $u = $Uri.Trim().TrimEnd('/')
    if ($u -match '^(.+/.+):([^/]+)$') {
        return $Matches[1]
    }
    return $u
}

$RootDir = Split-Path $PSScriptRoot -Parent
Set-Location $RootDir

if ($EcrUri) {
    $normalized = ConvertTo-EcrRepositoryUri -Uri $EcrUri
    if ($normalized -ne $EcrUri.Trim().TrimEnd('/')) {
        Write-Host "EcrUri: stripped image tag; using repository '$normalized'" -ForegroundColor Yellow
    }
    $EcrUri = $normalized
    if ($EcrUri -match ':') {
        throw @"
Invalid -EcrUri '$EcrUri'. Use repository only, e.g.:
  322725461022.dkr.ecr.ap-southeast-1.amazonaws.com/mychat-app
Tags are added automatically (gateway-$Tag, auth-$Tag, ...).
"@
    }
}

$definitions = @(
    @{ Key = 'gateway';      Dockerfile = 'api-gateway/Dockerfile';         Context = '.';              ImageSuffix = 'gateway' }
    @{ Key = 'auth';         Dockerfile = 'auth-service/Dockerfile';        Context = '.';              ImageSuffix = 'auth' }
    @{ Key = 'chat';         Dockerfile = 'chat-service/Dockerfile';        Context = '.';              ImageSuffix = 'chat' }
    @{ Key = 'user';         Dockerfile = 'user-service/Dockerfile';        Context = '.';              ImageSuffix = 'user' }
    @{ Key = 'notification'; Dockerfile = 'notification-service/Dockerfile'; Context = '.';              ImageSuffix = 'notification' }
    @{ Key = 'media';        Dockerfile = 'media-service/Dockerfile';       Context = '.';              ImageSuffix = 'media' }
    @{ Key = 'magika';       Dockerfile = 'magika-service/Dockerfile';      Context = 'magika-service'; ImageSuffix = 'magika' }
)

$toBuild = if ($Service -eq 'all') { $definitions } else { $definitions | Where-Object { $_.Key -eq $Service } }

if (-not $toBuild) {
    throw "Unknown service: $Service"
}

foreach ($def in $toBuild) {
    $localTag = "${ImagePrefix}:$($def.ImageSuffix)-$Tag"
    Write-Host "`n=== Building $localTag ===" -ForegroundColor Cyan
    Write-Host "  docker build -f $($def.Dockerfile) -t $localTag $($def.Context)" -ForegroundColor DarkGray

    docker build -f $def.Dockerfile -t $localTag $def.Context
    if ($LASTEXITCODE -ne 0) {
        throw "docker build failed for $($def.Key)"
    }

    if ($EcrUri) {
        $remoteTag = "$EcrUri`:$($def.ImageSuffix)-$Tag"
        Write-Host "  docker tag $localTag $remoteTag" -ForegroundColor DarkGray
        docker tag $localTag $remoteTag
        Write-Host "  docker push $remoteTag" -ForegroundColor DarkGray
        docker push $remoteTag
        if ($LASTEXITCODE -ne 0) {
            throw "docker push failed for $($def.Key)"
        }
    }
}

Write-Host "`nDone." -ForegroundColor Green

if (-not $EcrUri) {
    Write-Host @"

ECR push (example):
  aws ecr get-login-password --region ap-southeast-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.ap-southeast-1.amazonaws.com
  .\scripts\docker-build.ps1 -Service all -Tag v1 -EcrUri <account>.dkr.ecr.ap-southeast-1.amazonaws.com/mychat-app

"@ -ForegroundColor DarkGray
}
