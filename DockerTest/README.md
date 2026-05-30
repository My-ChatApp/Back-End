# DockerTest (wrapper)

Stack đầy đủ nằm ở thư mục cha:

- Infra: [`../docker-compose.yml`](../docker-compose.yml) — postgres, rabbitmq, valkey
- Apps: [`../docker-compose.apps.yml`](../docker-compose.apps.yml) — 7 image `mychatapp:*`
- Env: [`../.env.docker.example`](../.env.docker.example) → copy thành `.env.docker`

## Chạy local (build image trên máy)

```powershell
cd Back-End-NDuy

# 1. Build tất cả image
.\scripts\docker-build.ps1 -Service all -Tag v1

# 2. Env cho container
copy .env.docker.example .env.docker
# Chỉnh AWS_*, S3_BUCKET, JWT_SECRET (có thể copy từ .env)

# 3. Up
.\scripts\compose-up.ps1
# hoặc: .\DockerTest\up.ps1
```

## Dừng

```powershell
.\scripts\compose-down.ps1 -AppsOnly   # chỉ app
.\scripts\compose-down.ps1             # app + infra
```

## Kiểm tra

- Gateway: http://localhost:8080/api/auth/health
- RabbitMQ UI: http://localhost:15673 (guest/guest)
