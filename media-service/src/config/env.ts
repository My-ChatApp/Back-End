import dotenv from "dotenv";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

dotenv.config({ path: path.resolve(__dirname, "../../../.env") });

function readInt(name: string, defaultValue: number): number {
  const raw = process.env[name];
  if (raw === undefined || raw === "") {
    return defaultValue;
  }
  const value = Number.parseInt(raw, 10);
  return Number.isFinite(value) ? value : defaultValue;
}

function readFloat(name: string, defaultValue: number): number {
  const raw = process.env[name];
  if (raw === undefined || raw === "") {
    return defaultValue;
  }
  const value = Number.parseFloat(raw);
  return Number.isFinite(value) ? value : defaultValue;
}

function readBool(name: string, defaultValue: boolean): boolean {
  const raw = process.env[name]?.trim().toLowerCase();
  if (raw === undefined || raw === "") {
    return defaultValue;
  }
  return raw === "1" || raw === "true" || raw === "yes";
}

export const env = {
  port: readInt("MEDIA_SERVICE_PORT", 8085),
  awsRegion: process.env.AWS_REGION?.trim() || "ap-southeast-1",
  awsAccessKeyId: process.env.AWS_ACCESS_KEY_ID?.trim() ?? "",
  awsSecretAccessKey: process.env.AWS_SECRET_ACCESS_KEY?.trim() ?? "",
  s3Bucket: process.env.S3_BUCKET?.trim() ?? "",
  mediaPublicBaseUrl: process.env.MEDIA_PUBLIC_BASE_URL?.trim() ?? "",
  mediaPresignExpirationMinutes: readInt(
    "MEDIA_PRESIGN_EXPIRATION_MINUTES",
    15,
  ),
  /** HTTP sidecar (Docker service `magika`, port 8090). */
  magikaServiceUrl:
    process.env.MAGIKA_SERVICE_URL?.trim() || "http://localhost:8090",
  magikaRequestTimeoutMs: readInt("MAGIKA_REQUEST_TIMEOUT_MS", 10_000),
  magikaRetryMaxAttempts: readInt("MAGIKA_RETRY_MAX_ATTEMPTS", 3),
  magikaRetryDelayMinMs: readInt("MAGIKA_RETRY_DELAY_MIN_MS", 3000),
  magikaRetryDelayMaxMs: readInt("MAGIKA_RETRY_DELAY_MAX_MS", 5000),
  /** Health check at startup — magika container may still be starting. */
  magikaStartupRetryMaxAttempts: readInt(
    "MAGIKA_STARTUP_RETRY_MAX_ATTEMPTS",
    10,
  ),
  /** Client rate limit for outbound magika /identify (fixed window). */
  magikaClientRateLimitMax: readInt("MAGIKA_CLIENT_RATE_LIMIT_MAX", 5),
  magikaClientRateLimitWindowMs: readInt(
    "MAGIKA_CLIENT_RATE_LIMIT_WINDOW_MS",
    60_000,
  ),
  /** AWS SDK standard retry (S3 presign) — not fixed 3–5s backoff. */
  awsSdkMaxAttempts: readInt("AWS_SDK_MAX_ATTEMPTS", 3),
  /** @deprecated Use media-upload.config.json → fileHeadMaxBytes */
  mediaMagikaSampleMaxBytes: readInt("MEDIA_MAGIKA_SAMPLE_MAX_BYTES", 4096),
  mediaMagikaMinConfidence: readFloat("MEDIA_MAGIKA_MIN_CONFIDENCE", 0),
  /** Skip magika-service call when magic bytes already match extension (default true). */
  mediaMagikaSkipOnMagicMatch: readBool(
    "MEDIA_MAGIKA_SKIP_ON_MAGIC_MATCH",
    true,
  ),
} as const;
