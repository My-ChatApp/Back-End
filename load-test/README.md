# Load test — MyChatApp

Hai kịch bản:

| Công cụ | Mô phỏng |
|---------|----------|
| **JMeter** (`run-jmeter.ps1`) | HTTP: signin → conversations → GET messages → friends (lặp nhiều vòng) |
| **Node WS** (`run-ws-load.ps1`) | Giống user thật hơn: signin → **5 tin STOMP** → GET messages (tối đa **500 user cùng lúc**) |

**Không** gồm: upload media/Magika.

## Yêu cầu

| Công cụ | Phiên bản gợi ý |
|---------|------------------|
| [Apache JMeter](https://jmeter.apache.org/download_browser.html) | 5.6+ (`jmeter` / `jmeter.bat` trên PATH hoặc `JMETER_HOME`) |
| Node.js | 18+ (script sinh dữ liệu) |
| Docker | Postgres + stack app (xem [DOCKER.md](../DOCKER.md)) |

### Cài JMeter trên Windows (nếu chưa có)

```powershell
# Cách 1 — winget (khuyến nghị)
.\load-test\scripts\install-jmeter.ps1

# Cách 2 — thủ công
winget install --id DEVCOM.JMeter -e --accept-package-agreements --accept-source-agreements
```

Sau khi cài, **mở terminal mới** hoặc chỉ đường dẫn trực tiếp:

```powershell
# Sau winget, thường nằm tại:
.\load-test\scripts\run-jmeter.ps1 -JmeterHome "$env:LOCALAPPDATA\Programs\JMeter" -Threads 10 -RampUp 10 -Loops 2
```

Tải zip thủ công: [jmeter.apache.org/download](https://jmeter.apache.org/download_jmeter.cgi) → giải nén → `-JmeterHome` trỏ tới thư mục gốc (có `bin\jmeter.bat`).

## Cấu trúc

```
load-test/
├── README.md
├── package.json
├── data/                    # generated (gitignored: users.csv, seed.sql)
├── jmeter/
│   ├── MyChatApp-500users-http.jmx
│   └── users.csv.example
├── results/                 # JTL + HTML report (local)
└── scripts/
    ├── generate-loadtest-data.mjs
    ├── apply-seed.ps1
    ├── run-jmeter.ps1
    ├── run-jmeter.sh
    ├── ws-send-then-get.mjs
    ├── run-ws-load.ps1
    └── run-ws-load.sh
```

## 1. Chuẩn bị môi trường

1. Chạy infra + services (từ thư mục `Back-End`):

   ```powershell
   docker compose up -d
   .\scripts\compose-up.ps1   # hoặc run-dev / start từng service
   ```

2. Gateway phải trả lời tại `http://localhost:8080`.

3. Trong `.env`:

   - `GATEWAY_RATELIMIT_ENABLED=false` — tránh **429** khi 500 thread từ một IP.
   - `JWT_SECRET` cố định giữa các lần chạy.

4. DynamoDB Local + Valkey nên chạy nếu `GET .../messages` timeout (timeline rỗng vẫn hợp lệ).

## 2. Sinh dữ liệu & seed Postgres

```powershell
cd Back-End
npm install --prefix load-test
node load-test/scripts/generate-loadtest-data.mjs --count 500 --out-dir load-test/data
.\load-test\scripts\apply-seed.ps1
```

Tạo ra:

- `load-test/data/seed.sql` — 500 user `loadtest001@mychatapp.local` … `loadtest500@mychatapp.local`, mật khẩu chung **`LoadTest123!`**, 250 hội thoại PRIVATE (cặp 1–2, 3–4, …).
- `load-test/data/users.csv` — dùng cho JMeter.

Linux/macOS seed:

```bash
node load-test/scripts/generate-loadtest-data.mjs --count 500 --out-dir load-test/data
docker exec -i mychatapp-postgres psql -U mychatapp -d mychatapp < load-test/data/seed.sql
```

Chạy lại generator + seed là **idempotent** (xóa user/conversation loadtest cũ trước khi insert).

## 3. Load WebSocket — 5 tin / user rồi GET messages (khuyến nghị cho chat thật)

Mỗi user trong `users.csv`:

1. `POST /api/auth/signin`
2. SockJS + STOMP `CONNECT` → `http://localhost:8080/ws?access_token=...`
3. Gửi **5** lần `/app/chat.send` (TEXT)
4. Chờ `settle-ms` (mặc định 2s) cho Valkey/RabbitMQ/DynamoDB
5. `GET /api/conversations/{id}/messages?userId=...&limit=50`

### Cấu hình “500 user cùng lúc” (mặc định)

| Tham số | Mặc định | Ý nghĩa |
|---------|----------|---------|
| `Users` | `500` | Số user lấy từ CSV |
| `Concurrency` | `500` | Tối đa **500 phiên đang chạy** song song (cap) |
| `RampUpSec` | `180` | Trải thời điểm **bắt đầu** mỗi user trong 180s (giống JMeter ramp-up) |
| `Messages` | `5` | Số tin gửi qua WS / user |
| `SettleMs` | `2000` | Chờ sau khi gửi trước GET |
| `SendGapMs` | `0` | Khoảng cách giữa mỗi tin (`0` = burst; `150` = mô phỏng gõ chậm) |
| `ServerWaitMs` | `8000` | Chờ MESSAGE_CREATED sau khi gửi xong |

### Metric gửi tin (báo cáo HTML)

| Metric | Ý nghĩa |
|--------|---------|
| **sendWallMs** | Toàn phase gửi N tin (gồm sleep gap + chờ server từng tin) |
| **sendGapTotalMs** | Tổng thời gian `sleep` giữa các lần publish |
| **sendPublishMs** | `sendWall − sendGap` (thời gian client “làm việc”, không tính gap) |
| **avgServerRttMs** | Trung bình: publish → nhận `MESSAGE_CREATED` trên `/topic/conversation/{id}` |
| **serverRttMissed** | Số tin không nhận được event trong thời gian chờ |

```powershell
# Smoke 10 user
.\load-test\scripts\run-ws-load.ps1 -Users 10 -Concurrency 10 -RampUpSec 10 -Messages 5

# 500 user — profile production-like
.\load-test\scripts\run-ws-load.ps1 -Users 500 -Concurrency 500 -RampUpSec 180 -Messages 5
```

Kết quả:

- `load-test/results/ws-<timestamp>/summary.json`
- `user-results.jsonl`
- **`html-report/index.html`** — dashboard (giống JMeter, mở bằng trình duyệt)

Tạo lại HTML từ run cũ:

```powershell
node load-test/scripts/generate-ws-html-report.mjs --dir load-test/results/ws-20260601-221320
```

**Infra bắt buộc:** Postgres + **Valkey** + **RabbitMQ** + (nên có) **DynamoDB Local** — gửi tin kích hoạt persist async.

Trực tiếp bằng Node:

```powershell
node load-test/scripts/ws-send-then-get.mjs --users 500 --concurrency 500 --ramp-up-sec 180 --messages 5
```

## 4. Smoke test JMeter (10 users)

```powershell
.\load-test\scripts\run-jmeter.ps1 -Threads 10 -RampUp 10 -Loops 2
```

Kỳ vọng: error rate ~0%, `01 POST signin` và các GET đều **200**.

Git Bash:

```bash
chmod +x load-test/scripts/run-jmeter.sh
THREADS=10 RAMPUP=10 LOOPS=2 ./load-test/scripts/run-jmeter.sh
```

## 5. Chạy 500 users (JMeter HTTP)

```powershell
.\load-test\scripts\run-jmeter.ps1 -Threads 500 -RampUp 180 -Loops 20
```

Tham số JMeter (`-J`):

| Property | Mặc định | Mô tả |
|----------|----------|--------|
| `host` | `localhost` | Gateway host |
| `port` | `8080` | Gateway port |
| `threads` | `500` | Số users |
| `rampup` | `180` | Giây ramp-up |
| `loops` | `20` | Vòng lặp / user |
| `think_min` | `1000` | Think time min (ms) |
| `think_range` | `2000` | Random thêm (ms) → max ≈ 3000 |
| `csvfile` | `../data/users.csv` | Đường dẫn CSV (script truyền absolute) |

Kết quả: `load-test/results/<timestamp>/results.jtl` và `html-report/index.html`.

**Lưu ý:** Chạy **non-GUI** (`-n`); không bật View Results Tree với 500 thread (OOM). Script đặt `HEAP=-Xms1g -Xmx4g`.

## 6. Kịch bản JMeter (HTTP only)

1. `POST /api/auth/signin` → JSON `$.data.accessToken`
2. `GET /api/conversations/user/${userId}`
3. `GET /api/conversations/${conversationId}/messages?userId=...&limit=50`
4. `GET /api/friends?userId=...`

Mỗi thread đọc một dòng từ `users.csv` (shareMode.all — phân phối round-robin khi thread > số dòng; với 500 thread và 500 dòng mỗi user một dòng).

## 7. Đọc kết quả

- Mở `html-report/index.html`: throughput, error %, percentiles.
- Song song: RabbitMQ UI `http://localhost:15673`, `docker stats`, log `api-gateway` / `chat-service` nếu lỗi tăng.

Kết quả trên máy dev **không** thay SLA production AWS; dùng so sánh trước/sau tối ưu code.

## 8. CI

Load test **không** chạy trên GitHub Actions (phase 1). Chạy manual hoặc nightly trên máy có stack Docker đủ.

## Sự cố thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|------------|
| `summary = 0`, HTML report NPE | Đường dẫn project có **dấu cách** (`2025-2026 HK2`) — `-Jcsvfile` bị cắt nếu script không quote | Dùng `run-jmeter.ps1` mới nhất (splatting `-Jcsvfile=...`); xem `results/*/jmeter.log` |
| 401 sau signin | JWT / user không tồn tại | Chạy lại seed; kiểm tra email trong CSV |
| 429 | Rate limit gateway | `GATEWAY_RATELIMIT_ENABLED=false` |
| Signin fail hàng loạt | Chưa seed hoặc sai password | `LoadTest123!`, chạy `apply-seed.ps1` |
| `users.csv not found` | Chưa generate | `generate-loadtest-data.mjs` |
| OOM JMeter | Quá nhiều listener / heap nhỏ | Chỉ `-n`, tăng `-Xmx4g` |
| WS connect timeout | Gateway/chat down hoặc quá tải | Giảm `Concurrency`; tăng `RampUpSec` |
| `messageCount` &lt; 5 | `settle-ms` quá ngắn / DynamoDB chưa persist | Tăng `-SettleMs 3000`; bật DynamoDB + RabbitMQ |
| Send wall ~600ms nhưng gap=0 | Run cũ có `SendGapMs 150` | Regenerate report; mặc định mới `SendGapMs 0` |

## Mở rộng (phase sau)

- Presign media — cần nới `MAGIKA_CLIENT_RATE_LIMIT_*` và S3.
- Gộp metrics WS vào Influx/Grafana.
