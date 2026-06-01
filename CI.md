# CI — unit test, Docker build, push Docker Hub

Pipeline GitHub Actions: [`.github/workflows/backend-ci.yml`](../.github/workflows/backend-ci.yml)

## Luồng

1. **Job `test`** — Maven unit test (6 module Spring), không Postgres/RabbitMQ/Valkey.
2. **Job `build-images`** — chỉ chạy khi test pass; build 8 image.
   - **PR / `develop`**: tag local `mychatapp:<service>-ci-<sha>` (không push).
   - **Push `main`**: build + push lên Docker Hub repo một image, nhiều tag:
     `docker.io/baonguyen6f6562/mychatapp:<service>-ci-<sha>`  
     (ví dụ `gateway-ci-abc1234`, `auth-ci-abc1234`, …).

Dockerfile vẫn dùng `-DskipTests` khi build image; quality gate nằm ở job test.

### Secrets GitHub (bắt buộc cho push `main`)

| Secret | Mô tả |
|--------|--------|
| `DOCKERHUB_USERNAME` | `baonguyen6f6562` |
| `DOCKERHUB_TOKEN` | Access token từ [Docker Hub Security](https://hub.docker.com/settings/security) |

Tạo repo `mychatapp` trên Docker Hub (public hoặc private) trước khi push lần đầu.

## Phạm vi test

| Chạy trên CI | Không chạy |
|--------------|------------|
| Test Mockito / pure unit (`*Test.java` trừ `*ApplicationTests`) | `*ApplicationTests` (`@SpringBootTest` — cần infra hoặc H2 đầy đủ) |
| `common`, `api-gateway`, `auth`, `chat`, `user`, `notification` | `media-service`, `agent-service`, `magika-service` (chưa có test) |

Loại trừ Surefire: `**/*ApplicationTests.java`

## Chạy local

Cần bash (Git Bash / WSL) và JDK 21:

```bash
cd Back-End
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
