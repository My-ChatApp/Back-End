# CI — unit test, Docker build, push Docker Hub

Pipeline GitHub Actions: [`.github/workflows/backend-ci.yml`](./.github/workflows/backend-ci.yml)

## Luồng

1. **Job `test`** — Maven unit test (6 module Spring), không Postgres/RabbitMQ/Valkey.
2. **Job `build-images`** — chỉ chạy khi test pass; build 8 image.
   - **PR / `develop`**: tag local `mychatapp:<service>-ci-<sha>` (không push).
   - **Push `main`**: build + push lên Docker Hub repo một image, nhiều tag:
     `docker.io/baonguyen6f6562/mychatapp:<service>-ci-<sha>`  
     (ví dụ `gateway-ci-abc1234`, `auth-ci-abc1234`, …).
3. **Job `deploy`** — chỉ push `main`, sau khi build/push xong: SSH vào EC2, pull image Docker Hub, cập nhật `IMAGE_TAG` trong `.env.docker`, `docker compose up -d`.

Dockerfile vẫn dùng `-DskipTests` khi build image; quality gate nằm ở job test.

### Secrets GitHub (bắt buộc cho push `main`)

| Secret | Mô tả |
|--------|--------|
| `DOCKERHUB_USERNAME` | `baonguyen6f6562` |
| `DOCKERHUB_TOKEN` | Access token từ [Docker Hub Security](https://hub.docker.com/settings/security) |

Tạo repo `mychatapp` trên Docker Hub (public hoặc private) trước khi push lần đầu.

### Secrets GitHub (deploy EC2 qua SSH)

| Secret | Mô tả |
|--------|--------|
| `EC2_HOST` | Hostname hoặc IP public của EC2 |
| `EC2_USER` | Thường `ubuntu` |
| `EC2_SSH_PRIVATE_KEY` | Private key SSH (nội dung file `.pem`) |
| `EC2_APP_DIR` | *(tùy chọn)* Thư mục có `docker-compose.yml`. Tự nhận: `~/MyChatApp` (clone repo `Back-End`) hoặc `~/MyChatApp/Back-End` (monorepo) |

Security Group EC2 cần mở port **22** cho IP runner GitHub Actions (hoặc bastion/VPN).

### Chuẩn bị EC2 (một lần)

**Cách A — clone repo `Back-End` (như bạn đang dùng):**

```bash
git clone https://github.com/My-ChatApp/Back-End.git ~/MyChatApp
cd ~/MyChatApp
cp .env.docker.example .env.docker
# Chỉnh JWT, AWS, DB password, ...

chmod +x scripts/deploy-ec2.sh scripts/dockerhub-pull.sh scripts/compose-up.sh
```

Không có thư mục `Back-End` bên trong — toàn bộ code nằm ngay tại `~/MyChatApp`.

**Cách B — monorepo có thư mục `Back-End`:**

```bash
git clone <monorepo-url> ~/MyChatApp
cd ~/MyChatApp/Back-End
cp .env.docker.example .env.docker
chmod +x scripts/deploy-ec2.sh scripts/dockerhub-pull.sh
```

Mỗi lần deploy, CI `git pull` trong repo git (cùng thư mục clone) rồi chạy script.


Deploy thủ công (cùng lệnh CI chạy trên server):

```bash
export DOCKERHUB_USERNAME=...
export DOCKERHUB_TOKEN=...
./scripts/deploy-ec2.sh --tag ci-<full-git-sha>
```

Script: `scripts/dockerhub-pull.sh` (chỉ pull/tag), `scripts/deploy-ec2.sh` (pull + compose).

## Phạm vi test

| Chạy trên CI | Không chạy |
|--------------|------------|
| Test Mockito / pure unit (`*Test.java` trừ `*ApplicationTests`) | `*ApplicationTests` (`@SpringBootTest` — cần infra hoặc H2 đầy đủ) |
| `common`, `api-gateway`, `auth`, `chat`, `user`, `notification` | `media-service`, `agent-service`, `magika-service` (chưa có test) |

Loại trừ Surefire: `**/*ApplicationTests.java`

## Chạy local

Cần bash (Git Bash / WSL) và JDK 21:

```bash
# từ root repo Back-End
chmod +x scripts/ci-test.sh mvnw
./scripts/ci-test.sh
```

Build image sau khi test pass (không push):

```bash
IMAGE_TAG=ci-local ./scripts/ci-build-images.sh
```

Build + push Docker Hub (sau `docker login`):

```bash
IMAGE_PREFIX=docker.io/baonguyen6f6562/mychatapp IMAGE_TAG=v1 PUSH=1 ./scripts/ci-build-images.sh
```

Trên Windows có thể dùng WSL hoặc Git Bash cho các script `.sh`.

## Mở rộng sau

- Bật `*ApplicationTests` khi có `application-test.yml` + H2/mock cho mọi service.
- Vitest cho `media-service`; pytest cho Python services.
- Tag `latest` / semver trên `main` khi release.

Xem thêm: [`DOCKER.md`](./DOCKER.md)
