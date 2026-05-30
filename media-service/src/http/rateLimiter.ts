import { env } from '../config/env.js';

type WindowState = { count: number; windowStartMs: number };

/**
 * Fixed-window rate limiter: at most maxCalls requests per windowMs.
 * Returns 0 if allowed, or seconds until the window resets (>= 1) if blocked.
 */
export class FixedWindowRateLimiter {
  private state: WindowState = { count: 0, windowStartMs: Date.now() };

  constructor(
    private readonly maxCalls: number,
    private readonly windowMs: number
  ) {}

  tryAcquire(): number {
    const now = Date.now();
    if (now - this.state.windowStartMs >= this.windowMs) {
      this.state = { count: 0, windowStartMs: now };
    }

    if (this.state.count >= this.maxCalls) {
      const retryAfterMs = this.state.windowStartMs + this.windowMs - now;
      return Math.max(1, Math.ceil(retryAfterMs / 1000));
    }

    this.state.count += 1;
    return 0;
  }
}

export const magikaIdentifyRateLimiter = new FixedWindowRateLimiter(
  env.magikaClientRateLimitMax,
  env.magikaClientRateLimitWindowMs
);
