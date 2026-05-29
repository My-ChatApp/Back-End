export const RETRYABLE_HTTP_STATUSES = new Set([429, 502, 503, 504]);

export class HttpResponseError extends Error {
  constructor(
    message: string,
    readonly status: number
  ) {
    super(message);
    this.name = 'HttpResponseError';
  }
}

export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export function randomInt(min: number, max: number): number {
  const lo = Math.min(min, max);
  const hi = Math.max(min, max);
  return lo + Math.floor(Math.random() * (hi - lo + 1));
}

export function isRetryableHttpError(err: unknown, status?: number): boolean {
  if (typeof status === 'number') {
    return RETRYABLE_HTTP_STATUSES.has(status);
  }

  if (err instanceof HttpResponseError) {
    return RETRYABLE_HTTP_STATUSES.has(err.status);
  }

  if (err instanceof Error) {
    if (err.name === 'AbortError' || err.name === 'TimeoutError') {
      return true;
    }
    const code = (err as NodeJS.ErrnoException).code;
    if (
      code === 'ECONNREFUSED' ||
      code === 'ECONNRESET' ||
      code === 'ETIMEDOUT' ||
      code === 'ENOTFOUND' ||
      code === 'EAI_AGAIN'
    ) {
      return true;
    }
    if (err instanceof TypeError) {
      return true;
    }
  }

  return false;
}

export interface WithRetryOptions {
  maxAttempts: number;
  delayMinMs: number;
  delayMaxMs: number;
  /** Prefix for console.warn on retry (e.g. "magika /identify"). */
  label?: string;
}

function formatError(err: unknown): string {
  if (err instanceof HttpResponseError) {
    return `${err.message} (HTTP ${err.status})`;
  }
  if (err instanceof Error) {
    return err.message;
  }
  return String(err);
}

/**
 * Runs fn up to maxAttempts times. Waits delayMinMs–delayMaxMs (jitter) before each retry.
 * Only retries when isRetryableHttpError matches the thrown error.
 */
export async function withRetry<T>(
  fn: () => Promise<T>,
  options: WithRetryOptions
): Promise<T> {
  const { maxAttempts, delayMinMs, delayMaxMs, label } = options;
  const attempts = Math.max(1, maxAttempts);
  let lastError: unknown;

  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      return await fn();
    } catch (err) {
      lastError = err;
      const retryable = isRetryableHttpError(err);
      const isLast = attempt >= attempts;

      if (!retryable || isLast) {
        throw err;
      }

      const delayMs = randomInt(delayMinMs, delayMaxMs);
      const scope = label ? ` (${label})` : '';
      console.warn(
        `[media-service] retry attempt ${attempt + 1}/${attempts}${scope} after ${delayMs}ms: ${formatError(err)}`
      );
      await sleep(delayMs);
    }
  }

  throw lastError;
}
