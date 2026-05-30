# Chay toan bo backend local (Windows PowerShell) — cau hinh: .env + application.yml
# Usage:
#   .\scripts\run-dev.ps1
#   .\scripts\run-dev.ps1
#   .\scripts\run-dev.ps1 -SkipDocker      # chi chay microservice (infra da chay san)
#   .\scripts\run-dev.ps1 -SkipBuild
#   .\scripts\run-dev.ps1 -SkipMagika      # bo qua magika Docker (port 8090)
#   .\scripts\run-dev.ps1 -SyncCdkEnv      # cap nhat S3_BUCKET, MEDIA_PUBLIC_BASE_URL tu CDK stack
#
# Hoac double-click / cmd:  run-dev.bat

param(
    [switch]$SkipDocker,
    [switch]$SkipBuild,
    [switch]$SyncCdkEnv,
    [switch]$SkipMagika
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path $PSScriptRoot -Parent
Set-Location $RootDir

$LauncherDir = Join-Path $PSScriptRoot ".launchers"

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Test-CommandExists([string]$Name) {
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Invoke-Docker([string[]]$DockerArgs) {
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & docker @DockerArgs 2>&1 | Out-Null
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $prevEap
    return $exitCode
}

function Remove-DockerContainerIfExists([string]$Name) {
    if ((Invoke-Docker @("container", "inspect", $Name)) -eq 0) {
        Invoke-Docker @("rm", "-f", $Name) | Out-Null
    }
}

function Wait-TcpPort([int]$Port, [int]$TimeoutSec = 90) {
    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $client.Connect("127.0.0.1", $Port)
            $client.Close()
            return $true
        } catch {
            Start-Sleep -Seconds 2
            $elapsed += 2
        } finally {
            if ($client) { $client.Dispose() }
        }
    }
    return $false
}

function Start-DevService(
    [string]$Name,
    [string]$RelativeDir,
    [int]$Port,
    [int]$DelaySec = 0,
    [ValidateSet("java", "node", "python")]
    [string]$Runtime = "java"
) {
    $serviceDir = (Resolve-Path (Join-Path $RootDir $RelativeDir)).Path

    if ($DelaySec -gt 0) {
        Start-Sleep -Seconds $DelaySec
    }

    if (-not (Test-Path $LauncherDir)) {
        New-Item -ItemType Directory -Path $LauncherDir -Force | Out-Null
    }

    $launcherPath = Join-Path $LauncherDir "$Name.ps1"

    if ($Runtime -eq "python") {
        $launcherContent = @"
`$host.UI.RawUI.WindowTitle = 'MyChatApp - $Name (:$Port)'
Set-Location -LiteralPath '$serviceDir'
Write-Host '[$Name] port $Port — uvicorn (Python/FastAPI)' -ForegroundColor Green
`$venv = Join-Path '$serviceDir' '.venv'
if (-not (Test-Path `$venv)) {
    Write-Host '[$Name] tao venv + pip install...' -ForegroundColor Yellow
    python -m venv `$venv
    if (`$LASTEXITCODE -ne 0) { exit `$LASTEXITCODE }
    & (Join-Path `$venv 'Scripts\pip.exe') install -r requirements.txt
    if (`$LASTEXITCODE -ne 0) { exit `$LASTEXITCODE }
}
`$env:MAGIKA_PORT = '$Port'
& (Join-Path `$venv 'Scripts\uvicorn.exe') app.main:app --host 0.0.0.0 --port $Port
if (`$LASTEXITCODE -ne 0) {
    Write-Host ''; Write-Host "[uvicorn thoat ma `$LASTEXITCODE]" -ForegroundColor Red
    Read-Host 'Nhan Enter de dong'
}
"@
    } elseif ($Runtime -eq "node") {
        $launcherContent = @"
`$host.UI.RawUI.WindowTitle = 'MyChatApp - $Name (:$Port)'
Set-Location -LiteralPath '$serviceDir'
Write-Host '[$Name] port $Port — cau hinh: ..\.env (Node/Express)' -ForegroundColor Green
if (-not (Test-Path 'node_modules')) {
    Write-Host '[$Name] npm install...' -ForegroundColor Yellow
    npm install
    if (`$LASTEXITCODE -ne 0) { exit `$LASTEXITCODE }
}
npm run dev
if (`$LASTEXITCODE -ne 0) {
    Write-Host ''; Write-Host "[npm thoat ma `$LASTEXITCODE]" -ForegroundColor Red
    Read-Host 'Nhan Enter de dong'
}
"@
    } else {
        $localMvnw = Join-Path $serviceDir "mvnw.cmd"
        $rootMvnw = Join-Path $RootDir "mvnw.cmd"

        if (Test-Path $localMvnw) {
            $mvnwPath = (Resolve-Path $localMvnw).Path
        } elseif (Test-Path $rootMvnw) {
            $mvnwPath = (Resolve-Path $rootMvnw).Path
        } else {
            throw "Khong tim thay mvnw.cmd (service hoac root Back-End-NDuy)"
        }

        $launcherContent = @"
`$host.UI.RawUI.WindowTitle = 'MyChatApp - $Name (:$Port)'
Set-Location -LiteralPath '$serviceDir'
Write-Host '[$Name] port $Port — cau hinh: application.yml + ..\.env' -ForegroundColor Green
& '$mvnwPath' spring-boot:run
if (`$LASTEXITCODE -ne 0) {
    Write-Host ''; Write-Host "[Maven thoat ma `$LASTEXITCODE]" -ForegroundColor Red
    Read-Host 'Nhan Enter de dong'
}
"@
    }

    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($launcherPath, $launcherContent, $utf8NoBom)

    # cmd "start" mo console moi ro rang (Start-Process pwsh tu Cursor/IDE doi khi khong hien cua so)
    $windowTitle = "MyChatApp - $Name"
    $psExe = (Get-Command powershell.exe -ErrorAction Stop).Source
    $startCmd = "start `"$windowTitle`" `"$psExe`" -NoExit -NoProfile -ExecutionPolicy Bypass -File `"$launcherPath`""
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $startCmd -WorkingDirectory $serviceDir -WindowStyle Hidden

    Write-Host "  Da mo cua so: $Name (port $Port) — tim tren taskbar: $windowTitle" -ForegroundColor DarkGray
}

function Start-MagikaDocker {
    param([int]$Port = 8090)

    $magikaImage = "mychatapp:magika-dev"
    $containerName = "mychatapp-magika-dev"
    $dockerfile = Join-Path $RootDir "magika-service\Dockerfile"
    $context = Join-Path $RootDir "magika-service"

    Remove-DockerContainerIfExists $containerName

    if ((Invoke-Docker @("image", "inspect", $magikaImage)) -ne 0) {
        Write-Host "  Build image magika (lan dau co the mat 2-5 phut)..." -ForegroundColor Yellow
        docker build -t $magikaImage -f $dockerfile $context
        if ($LASTEXITCODE -ne 0) {
            throw "docker build magika-service that bai"
        }
    }

    docker run -d --name $containerName -p "${Port}:8090" `
        -e MAGIKA_PORT=8090 -e MAGIKA_MAX_BYTES=4096 `
        $magikaImage | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "docker run magika that bai"
    }

    Write-Host "  Magika container: $containerName (port $Port)" -ForegroundColor DarkGray
    Write-Host "  Cho warm-up model (~10-30s)..." -ForegroundColor DarkGray

    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-RestMethod -Uri "http://127.0.0.1:${Port}/health" -TimeoutSec 3 -ErrorAction Stop
            if ($r.status -eq 'ok') {
                Write-Host "  Magika san sang (http://localhost:${Port}/health)" -ForegroundColor DarkGray
                return
            }
        } catch {
            Start-Sleep -Seconds 3
        }
    }
    Write-Host "  Canh bao: Magika chua tra /health — xem: docker logs $containerName" -ForegroundColor Yellow
}

Write-Host "MyChatApp Backend (application.yml + .env)" -ForegroundColor Green
Write-Host "Thu muc: $RootDir"

if (-not (Test-CommandExists "java")) {
    throw "Chua cai Java. Can JDK 21+ trong PATH."
}

if (-not (Test-CommandExists "npm")) {
    throw "Chua cai Node.js/npm (can cho media-service). Cai Node.js LTS hoac them npm vao PATH."
}

# 0. Dong bo .env tu CDK (tuy chon)
if ($SyncCdkEnv) {
    Write-Step "Dong bo .env tu CDK stack (S3, CloudFront)"
    if (Test-CommandExists "aws") {
        & (Join-Path $PSScriptRoot "sync-cdk-env.ps1")
    } else {
        Write-Host "  Bo qua: chua cai AWS CLI" -ForegroundColor Yellow
    }
}

# Kiem tra bien S3 bat buoc cho media + user avatar
$envPath = Join-Path $RootDir ".env"
if (Test-Path $envPath) {
    $envText = Get-Content $envPath -Raw
    if ($envText -notmatch '(?m)^MEDIA_PUBLIC_BASE_URL=.+') {
        Write-Host "  Canh bao: MEDIA_PUBLIC_BASE_URL trong .env dang trong." -ForegroundColor Yellow
        Write-Host "  Chay: .\scripts\deploy-infra.ps1 hoac .\scripts\sync-cdk-env.ps1" -ForegroundColor Yellow
    }
}

# 1. Docker (PostgreSQL + RabbitMQ + Valkey)
if (-not $SkipDocker) {
    Write-Step "Khoi dong Docker Compose (postgres, rabbitmq, valkey)"
    if (-not (Test-CommandExists "docker")) {
        throw "Chua cai Docker. Cai Docker Desktop hoac chay voi -SkipDocker neu da chay san."
    }

    docker compose up -d
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up that bai"
    }

    if (-not (Wait-TcpPort -Port 5433)) {
        Write-Host "  Canh bao: PostgreSQL Docker chua lang nghe port 5433" -ForegroundColor Yellow
    } else {
        Write-Host "  PostgreSQL Docker san sang (5433 — tranh xung dot PG may 5432)" -ForegroundColor DarkGray
    }
    if (-not (Wait-TcpPort -Port 5673)) {
        throw "RabbitMQ Docker chua san sang tai port 5673"
    }
    Write-Host "  RabbitMQ Docker san sang (5673, UI: http://localhost:15673)" -ForegroundColor DarkGray
    if (Wait-TcpPort -Port 6379 -TimeoutSec 30) {
        Write-Host "  Valkey san sang (6379)" -ForegroundColor DarkGray
    }
} else {
    Write-Step "Bo qua Docker (-SkipDocker)"
}

# 2. Build common
if (-not $SkipBuild) {
    Write-Step "Build module common (mvn install)"
    Push-Location (Join-Path $RootDir "common")
    .\mvnw.cmd install -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Pop-Location
        throw "Build common that bai"
    }
    Pop-Location
    Write-Host "  common da install vao local Maven repo" -ForegroundColor DarkGray

    Write-Step "npm install media-service (Node/Express)"
    Push-Location (Join-Path $RootDir "media-service")
    npm install
    if ($LASTEXITCODE -ne 0) {
        Pop-Location
        throw "npm install media-service that bai"
    }
    Pop-Location
    Write-Host "  media-service dependencies san sang" -ForegroundColor DarkGray
} else {
    Write-Step "Bo qua build (-SkipBuild)"
}

# 2b. Magika sidecar (Docker — media-service goi MAGIKA_SERVICE_URL)
if (-not $SkipMagika) {
    if (-not (Test-CommandExists "docker")) {
        Write-Host "  Canh bao: bo qua magika — chua co Docker" -ForegroundColor Yellow
    } else {
        Write-Step "Khoi dong magika-service (Docker, port 8090)"
        Start-MagikaDocker -Port 8090
    }
} else {
    Write-Step "Bo qua magika (-SkipMagika)"
}

# 3. Microservices (gateway chay sau cung)
Write-Step "Khoi dong cac microservice (moi service mot cua so PowerShell)"

$services = @(
    @{ Name = "auth-service";         Dir = "auth-service";         Port = 8081; Delay = 0;  Runtime = "java" },
    @{ Name = "chat-service";         Dir = "chat-service";         Port = 8082; Delay = 3;  Runtime = "java" },
    @{ Name = "user-service";         Dir = "user-service";         Port = 8083; Delay = 0;  Runtime = "java" },
    @{ Name = "media-service";        Dir = "media-service";        Port = 8085; Delay = 0;  Runtime = "node" },
    @{ Name = "notification-service"; Dir = "notification-service"; Port = 8084; Delay = 0;  Runtime = "java" },
    @{ Name = "api-gateway";          Dir = "api-gateway";          Port = 8080; Delay = 8;  Runtime = "java" }
)

foreach ($svc in $services) {
    $runtime = if ($svc.Runtime) { $svc.Runtime } else { "java" }
    Start-DevService -Name $svc.Name -RelativeDir $svc.Dir -Port $svc.Port -DelaySec $svc.Delay -Runtime $runtime
}

Write-Step "Hoan tat"
Write-Host @"

Cac dich vu:
  API Gateway       http://localhost:8080
  Auth              http://localhost:8081
  Chat              http://localhost:8082
  User (+ Friends)  http://localhost:8083
  Media (Node/S3)    http://localhost:8085  -> /api/media/presigned-upload
  Notification      http://localhost:8084
  Magika (Docker)   http://localhost:8090/health
  RabbitMQ UI       http://localhost:15673  (guest/guest)
  Valkey            localhost:6379

Infra S3/CDN:
  .\scripts\deploy-infra.ps1     # CDK deploy + cap nhat .env
  .\scripts\sync-cdk-env.ps1      # chi cap nhat S3_BUCKET, MEDIA_PUBLIC_BASE_URL

Dung lenh sau de dung:
  .\scripts\stop-dev.ps1

"@ -ForegroundColor Yellow
