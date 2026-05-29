# magika-service (Docker)

Sidecar HTTP API wrapping [Google Magika](https://github.com/google/magika) (Rust engine via Python package).  
Model loads once at container start, then one dummy inference warms JIT/tensor pools before traffic.

## API

| Method | Path | Body |
|--------|------|------|
| `GET` | `/health` | — |
| `POST` | `/identify` | Raw bytes (`application/octet-stream`) **or** JSON `{ "data": "<base64>" }` |

Response:

```json
{
  "label": "png",
  "mime": "image/png",
  "score": 0.99,
  "description": "PNG image",
  "isText": false
}
```

Max sample size: `MAGIKA_MAX_BYTES` (default `4096`).

## Build & run (standalone)

```bash
cd Back-End-NDuy/magika-service
docker build -t mychatapp-magika .
docker run --rm -p 8090:8090 mychatapp-magika
```

First start may take ~10–30s (model load + warm-up). Watch logs for `warm-up done`.

## Via root docker compose

```bash
cd Back-End-NDuy
docker compose up -d magika
```

Service: `http://localhost:8090`

## Quick test

```bash
# health
curl -s http://localhost:8090/health

# PNG signature (8 bytes) as JSON base64
curl -s -X POST http://localhost:8090/identify \
  -H "Content-Type: application/json" \
  -d "{\"data\":\"iVBORw0KGgo=\"}"
```

PowerShell:

```powershell
Invoke-RestMethod http://localhost:8090/health
$png = [Convert]::ToBase64String([byte[]](0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A))
Invoke-RestMethod -Method Post -Uri http://localhost:8090/identify `
  -ContentType application/json -Body (@{ data = $png } | ConvertTo-Json)
```

## media-service integration

`media-service` calls this sidecar via `MAGIKA_SERVICE_URL` (default `http://localhost:8090`).  
On a shared Docker network use `http://magika:8090`.
