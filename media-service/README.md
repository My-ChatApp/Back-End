# media-service (Node.js / Express / TypeScript)

Presigned S3 upload API using **Pipes & Filters** architecture: a single `PresignMessage` flows through independent filters (Producer → Transformers → Testers → Consumer).

## Pipeline topology

| Kind | Filter | Role |
|------|--------|------|
| Producer | `PresignInputProducer` | Entry (no-op when message built in `createInitialMessage`) |
| Transformer | `AttachUploadStrategyTransformer` | Attach purpose strategy |
| Tester | `MediaConfigReadyTester` | S3 bucket + public URL configured |
| Tester | `SizeLimitTester` | `contentLength` vs max |
| Tester | `ExtensionAllowlistTester` | Extension allowlist |
| Transformer | `DecodeFileHeadTransformer` | `fileHeadBase64` → buffer |
| Transformer | `MagicSniffTransformer` | Magic-byte sniff → `sniffedMime` |
| Tester | `ExtensionMagicMatchTester` | Extension vs sniff |
| Transformer | `MagikaIdentifyOrSkipTransformer` | Magika HTTP or skip → `detectedMime` |
| Tester | `MimeAllowlistTester` | MIME allowlist |
| Tester | `ExtensionMimeMatchTester` | Extension vs detected MIME |
| Transformer | `BuildS3ObjectKeyTransformer` | S3 object key |
| Transformer | `S3PresignTransformer` | Presigned PUT URL |
| Consumer | `PresignResponseConsumer` | Ensure response present |

Stage order is fixed in [`src/pipeline/presign/buildPresignPipeline.ts`](src/pipeline/presign/buildPresignPipeline.ts). Entry point: [`runPresignPipeline.ts`](src/pipeline/presign/runPresignPipeline.ts).

## Cấu hình loại file (quan trọng)

Chỉnh **`media-upload.config.json`** ở thư mục gốc `media-service` (không cần sửa code TypeScript):

```json
{
  "fileHeadMaxBytes": 4096,
  "purposes": {
    "message": {
      "maxBytes": 52428800,
      "types": [
        { "extension": ".jpg", "mime": "image/jpeg", "magikaLabel": "jpeg" }
      ]
    }
  }
}
```

| Trường | Ý nghĩa |
|--------|---------|
| `fileHeadMaxBytes` | Kích thước tối đa mẫu `fileHeadBase64` gửi lên |
| `purposes.<name>.maxBytes` | Dung lượng tối đa theo mục đích upload |
| `types[].extension` | Đuôi file (bắt buộc có dấu `.`) |
| `types[].mime` | MIME dùng cho S3 presign |
| `types[].magikaLabel` | (tuỳ chọn) Nhãn Magika map sang MIME khi sidecar trả `zip` |

Đường dẫn khác: biến môi trường `MEDIA_UPLOAD_CONFIG_PATH`.

Sau khi sửa JSON: **restart** `media-service`.

## Prerequisites

```bash
cd Back-End-NDuy
docker compose up -d magika
```

## Endpoint

- `POST /api/media/presigned-upload`
- Requires `X-UserId` from api-gateway.
- Body: `{ purpose, fileName?, contentType, contentLength, fileHeadBase64 }`

## Client rate limit (magika `/identify`)

Trước mỗi lần gọi **magika** `POST /identify`, `media-service` áp dụng **fixed window** (`src/http/rateLimiter.ts`):

- Mặc định tối đa **5** lần / **60** giây (`MAGIKA_CLIENT_RATE_LIMIT_MAX`, `MAGIKA_CLIENT_RATE_LIMIT_WINDOW_MS`).
- Vượt ngưỡng → không gọi magika; API presign trả **429** + `code: RATE_LIMIT_EXCEEDED`.
- **`/health` startup** không tính vào quota.

In-memory theo process — nhiều replica media-service có thể vượt tổng 5/phút; production có thể chuyển sang Valkey sau.

## Retry (magika-service & S3)

Gọi **magika-service** (`/health`, `/identify`) dùng retry tập trung trong `src/http/retry.ts`:

- Chờ **3–5 giây** (jitter) giữa các lần thử khi lỗi tạm thời: timeout, lỗi mạng, HTTP `429`, `502`, `503`, `504`.
- **Không** retry lỗi `4xx` (validation) hoặc lỗi nghiệp vụ sau khi response OK (ví dụ MIME/confidence).
- Startup `/health` dùng `MAGIKA_STARTUP_RETRY_MAX_ATTEMPTS` (mặc định `10`) để chờ container magika sẵn sàng.

**S3 presign** dùng retry mặc định của AWS SDK (`AWS_SDK_MAX_ATTEMPTS`, backoff exponential — không phải delay cố định 3–5s).

## Biến môi trường (`.env`)

| Variable | Description |
|----------|-------------|
| `MEDIA_SERVICE_PORT` | Default `8085` |
| `MAGIKA_SERVICE_URL` | Sidecar (default `http://localhost:8090`) |
| `MAGIKA_REQUEST_TIMEOUT_MS` | Timeout mỗi lần gọi magika (default `10000`) |
| `MAGIKA_RETRY_MAX_ATTEMPTS` | Tổng lần thử `/identify` (default `3`) |
| `MAGIKA_RETRY_DELAY_MIN_MS` / `MAGIKA_RETRY_DELAY_MAX_MS` | Khoảng chờ giữa retry (default `3000`–`5000`) |
| `MAGIKA_STARTUP_RETRY_MAX_ATTEMPTS` | Retry `/health` lúc startup (default `10`) |
| `MAGIKA_CLIENT_RATE_LIMIT_MAX` | Client RL: tối đa lần gọi `/identify` / cửa sổ (default `5`) |
| `MAGIKA_CLIENT_RATE_LIMIT_WINDOW_MS` | Client RL: độ dài cửa sổ ms (default `60000`) |
| `AWS_SDK_MAX_ATTEMPTS` | Retry S3 qua SDK (default `3`) |
| `MEDIA_MAGIKA_SKIP_ON_MAGIC_MATCH` | Bỏ qua Magika nếu magic khớp extension |
| `MEDIA_UPLOAD_CONFIG_PATH` | Tuỳ chọn — file JSON cấu hình upload |
| `S3_BUCKET`, `MEDIA_PUBLIC_BASE_URL`, AWS keys | S3 presign |

## Run locally

```bash
docker compose up -d magika
cd media-service && npm install && npm run dev
```
