import { env } from '../config/env.js';
import { MediaUploadError } from '../errors/mediaErrors.js';
import { magikaIdentifyRateLimiter } from '../http/rateLimiter.js';
import { HttpResponseError, withRetry, type WithRetryOptions } from '../http/retry.js';

export interface MagikaIdentifyResult {
  label: string;
  mime: string;
  score: number;
}

function baseUrl(): string {
  return env.magikaServiceUrl.replace(/\/$/, '');
}

function magikaRetryOptions(overrides?: Partial<WithRetryOptions>): WithRetryOptions {
  return {
    maxAttempts: env.magikaRetryMaxAttempts,
    delayMinMs: env.magikaRetryDelayMinMs,
    delayMaxMs: env.magikaRetryDelayMaxMs,
    ...overrides,
  };
}

async function magikaFetch(
  path: string,
  init: RequestInit,
  retryOverrides?: Partial<WithRetryOptions>
): Promise<Response> {
  const label = retryOverrides?.label ?? path;
  const options = magikaRetryOptions({ ...retryOverrides, label });

  return withRetry(async () => {
    const response = await fetch(`${baseUrl()}${path}`, {
      ...init,
      signal: AbortSignal.timeout(env.magikaRequestTimeoutMs),
    });

    if (!response.ok) {
      const detail = await response.text();
      throw new HttpResponseError(
        `magika-service ${path} failed (${response.status}): ${detail}`,
        response.status
      );
    }

    return response;
  }, options);
}

/** Fails startup if the magika-service sidecar is not reachable. */
export async function checkMagikaServiceHealth(): Promise<void> {
  const response = await magikaFetch('/health', { method: 'GET' }, {
    maxAttempts: env.magikaStartupRetryMaxAttempts,
    label: 'magika /health',
  });

  const body = (await response.json()) as { status?: string };
  if (body.status !== 'ok') {
    throw new Error('magika-service is not ready');
  }
}

export async function identifyBytesViaMagika(buffer: Buffer): Promise<MagikaIdentifyResult> {
  const retryAfterSec = magikaIdentifyRateLimiter.tryAcquire();
  if (retryAfterSec > 0) {
    throw new MediaUploadError(
      'RATE_LIMIT_EXCEEDED',
      `Đã vượt giới hạn kiểm tra file (tối đa ${env.magikaClientRateLimitMax} lần/phút). Thử lại sau ${retryAfterSec} giây.`
    );
  }

  const response = await magikaFetch(
    '/identify',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ data: buffer.toString('base64') }),
    },
    { label: 'magika /identify' }
  );

  const body = (await response.json()) as {
    label?: string;
    mime?: string;
    score?: number;
  };

  const mime = body.mime?.trim();
  if (!mime) {
    throw new Error('magika-service returned no mime');
  }

  const score = typeof body.score === 'number' ? body.score : 0;
  if (env.mediaMagikaMinConfidence > 0 && score < env.mediaMagikaMinConfidence) {
    throw new Error(`Magika confidence too low: ${score}`);
  }

  return {
    label: body.label ?? 'unknown',
    mime,
    score,
  };
}
