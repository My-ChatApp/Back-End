# Docker — môi trường local (PostgreSQL + DynamoDB)

Hạ tầng dev chạy bằng Docker; ứng dụng Spring Boot chạy trên máy (hoặc container sau).  
Schema SQL: [`designDB.md`](./designDB.md) · DynamoDB single-table: `MyChatApp_Chat`.

## Services

| Service | Port | Mục đích |
|---------|------|----------|
| `postgres` | **5433** (host) → 5432 (container) | PostgreSQL 16 — tránh xung đột PG cài trên Windows (port 5432) |
| `dynamodb-local` | 8000 | DynamoDB Local (API) |
| `dynamodb-admin` | 8001 | UI xem bảng/item |
| `rabbitmq` | **5673**, **15673** (host) | Message queue Docker — tránh xung đột RabbitMQ Windows (5672/15672) |
| `valkey` | **6379** | Valkey 8 (space-based chat timeline) — Redis-compatible API |
| `magika` | **8090** | Magika sidecar — chạy qua [`DockerTest`](./DockerTest/README.md) hoặc build image (xem bên dưới) |

## Khởi động

```bash
cd Back-End-NDuy

# 1. Copy biến môi trường (tùy chọn)
cp .env.example .env

# 2. Chạy infra
docker compose up -d

# 3. Tạo bảng DynamoDB (cần AWS CLI)
# Linux/macOS:
bash infra/dynamodb/create-table.sh

# Windows PowerShell:
powershell -File infra/dynamodb/create-table.ps1
```

PostgreSQL: schema tự chạy lần đầu từ `infra/postgres/init/*.sql` (chỉ khi volume `postgres_data` mới).

### Kiểm tra

- PostgreSQL: `docker exec -it mychatapp-postgres psql -U mychatapp -d mychatapp -c "\dt app.*"`
- DynamoDB UI: http://localhost:8001
- RabbitMQ UI: http://localhost:15673 (guest/guest)
- Valkey: `docker exec -it mychatapp-valkey valkey-cli ping` → `PONG`
- Magika: `curl -s http://localhost:8090/health` (DockerTest: `.\DockerTest\up.ps1`, hoặc `docker build -f magika-service/Dockerfile -t mychatapp:magika-v1 ./magika-service` — xem [`magika-service/README.md`](./magika-service/README.md))

### Valkey / ElastiCache (production)

Local: `SPRING_DATA_REDIS_HOST=localhost`, `SPRING_DATA_REDIS_SSL_ENABLED=false`.

AWS ElastiCache for Valkey: set primary endpoint, `SPRING_DATA_REDIS_SSL_ENABLED=true`, và AUTH token trong `SPRING_DATA_REDIS_PASSWORD`. Code Spring không đổi — chỉ env (xem `.env.example`).

### API Gateway rate limit (Valkey)

Phân tán qua `RequestRateLimiter` + Valkey khi bật:

1. `docker compose up -d valkey`
2. Trong `Back-End-NDuy/.env`: `GATEWAY_RATELIMIT_ENABLED=true` (dùng chung `SPRING_DATA_REDIS_*`)
3. Restart `api-gateway` (port 8080)

**Fail-open:** `GATEWAY_RATELIMIT_ENABLED=false` (mặc định) — gateway không cần Valkey, không áp limit.

| Route | Key | Mặc định (req/s, burst) |
|-------|-----|-------------------------|
| `/api/auth/**` (trừ health) | IP | 5 / 10 |
| `/api/media/**` | `X-UserId` | 10 / 20 |
| `/ws/**` | IP | 50 / 100 |
| REST khác | `X-UserId` | 50 / 100 |
| `/api/auth/health` | — | không limit |

Vượt quota → HTTP **429** + headers `X-RateLimit-*`. Chỉnh qua `GATEWAY_RATELIMIT_*` trong `.env`.

## Kết nối từ Spring Boot

### File cấu hình

| File | Mô tả |
|------|--------|
| [`Back-End-NDuy/.env`](./.env) | Biến môi trường local (không commit secret thật) |
| `*/application.yml` | Cấu hình mặc định + placeholder `${ENV:default}` |

Mỗi service tự `import` file `Back-End-NDuy/.env` qua `application.yml`:

```yaml
spring.config.import: optional:file:../.env[.properties]
```

### Chạy service

```powershell
cd Back-End-NDuy
.\run-dev.bat
# hoặc từng service:
cd auth-service
..\mvnw.cmd spring-boot:run
```

**IntelliJ / VS Code:** Run Configuration → Env file → trỏ tới `Back-End-NDuy/.env` (không cần Spring profile).

| Service | Port |
|---------|------|
| api-gateway | 8080 |
| auth-service | 8081 |
| chat-service | 8082 |
| user-service | 8083 (+ `/api/friends`) |
| notification-service | 8084 |

**Lưu ý:** Code đã migrate sang **JPA/PostgreSQL** + **DynamoDB** (profile `local`). Chạy `docker compose up` + script tạo bảng DynamoDB trước khi start service.

## Reset dữ liệu local

```bash
docker compose down -v   # xóa volumes (mất data PG + DynamoDB)
docker compose up -d
bash infra/dynamodb/create-table.sh
```

Chỉ reset PostgreSQL schema (giữ volume):

```bash
docker compose down postgres
docker volume rm back-end-nduy_postgres_data   # tên volume có thể khác: docker volume ls
docker compose up -d postgres
```

## Chuyển sang AWS (sau)

| Local | AWS |
|-------|-----|
| `localhost:5432` | Amazon **RDS** PostgreSQL |
| `DYNAMODB_ENDPOINT=http://localhost:8000` | **Bỏ** endpoint → DynamoDB region thật |
| `mychatapp` / `mychatapp_local` | Secrets Manager + IAM |
| `create-table.sh` | CloudFormation / Terraform / Console |

Profile `prod` (gợi ý):

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}   # RDS
  # không có mychatapp.dynamodb.endpoint

aws:
  region: ap-southeast-1
```

Bảng DynamoDB và schema `app` trên RDS nên giữ **cùng tên/DDL** với local để giảm khác biệt môi trường.

## Chạy full stack local (image build trên máy)

| File | Nội dung |
|------|----------|
| `docker-compose.yml` | postgres, rabbitmq, valkey |
| `docker-compose.apps.yml` | gateway, auth, chat, user, notification, media, magika |
| `.env.docker` | biến môi trường container (copy từ `.env.docker.example`) |

```powershell
cd Back-End-NDuy

.\scripts\docker-build.ps1 -Service all -Tag v1
copy .env.docker.example .env.docker
.\scripts\compose-up.ps1
```

Dừng: `.\scripts\compose-down.ps1` · Chi tiết: [`DockerTest/README.md`](./DockerTest/README.md).

### EC2 / Ubuntu (pull từ ECR)

```bash
cd Back-End-NDuy
chmod +x scripts/ecr-pull.sh scripts/compose-up.sh scripts/compose-down.sh

# Pull 7 image + tag mychatapp:*-v1 (tự login ECR, dùng sudo nếu cần)
./scripts/ecr-pull.sh

# Hoặc chỉnh repo/tag:
./scripts/ecr-pull.sh --ecr 322725461022.dkr.ecr.ap-southeast-1.amazonaws.com/mychat-app --tag v1

cp .env.docker.example .env.docker   # chỉnh secret
./scripts/compose-up.sh
./scripts/compose-down.sh
```

## Container images (ECS / ECR)

Ứng dụng (Spring Boot, media-service, magika) có Dockerfile riêng; build từ thư mục `Back-End-NDuy` (trừ magika — context `magika-service/`).

| Service | Dockerfile | Port | Tag ECR gợi ý |
|---------|------------|------|----------------|
| api-gateway | `api-gateway/Dockerfile` | 8080 | `gateway-v1` |
| auth-service | `auth-service/Dockerfile` | 8081 | `auth-v1` |
| chat-service | `chat-service/Dockerfile` | 8082 | `chat-v1` |
| user-service | `user-service/Dockerfile` | 8083 | `user-v1` |
| notification-service | `notification-service/Dockerfile` | 8084 | `notification-v1` |
| media-service | `media-service/Dockerfile` | 8085 | `media-v1` |
| magika-service | `magika-service/Dockerfile` | 8090 | `magika-v1` |

### Build thủ công

```powershell
cd Back-End-NDuy

docker build -f auth-service/Dockerfile -t mychatapp:auth-v1 .
docker build -f api-gateway/Dockerfile -t mychatapp:gateway-v1 .
docker build -f chat-service/Dockerfile -t mychatapp:chat-v1 .
docker build -f user-service/Dockerfile -t mychatapp:user-v1 .
docker build -f notification-service/Dockerfile -t mychatapp:notification-v1 .
docker build -f media-service/Dockerfile -t mychatapp:media-v1 .
docker build -f magika-service/Dockerfile -t mychatapp:magika-v1 ./magika-service
```

### Script build (một repo ECR, nhiều tag)

```powershell
.\scripts\docker-build.ps1 -Service all -Tag v1

# Build + tag + push lên ECR
aws ecr get-login-password --region ap-southeast-1 `
  | docker login --username AWS --password-stdin <account>.dkr.ecr.ap-southeast-1.amazonaws.com

.\scripts\docker-build.ps1 -Service all -Tag v1 `
  -EcrUri <account>.dkr.ecr.ap-southeast-1.amazonaws.com/iuh-project-backend
```

**Lưu ý:**

- File [`.dockerignore`](./.dockerignore) loại trừ `.env`, `target/`, `node_modules/` khỏi build context.
- Trên ECS: inject biến môi trường (RDS, JWT, `AUTH_SERVICE_URI`, …) — không copy `.env` vào image.
- `media-service` cần `MAGIKA_SERVICE_URL` trỏ tới magika (ví dụ `http://magika:8090`) khi chạy container.

## Yêu cầu

- Docker Desktop / Docker Engine
- [AWS CLI v2](https://aws.amazon.com/cli/) (chỉ để tạo bảng DynamoDB local; credentials giả `local`/`local`)
