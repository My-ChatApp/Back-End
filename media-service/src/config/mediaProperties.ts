import { env } from './env.js';

export function normalizedPublicBaseUrl(): string {
  let base = env.mediaPublicBaseUrl;
  while (base.endsWith('/')) {
    base = base.slice(0, -1);
  }
  return base;
}

export function publicUrlForKey(key: string): string {
  return `${normalizedPublicBaseUrl()}/${key}`;
}

export function presignExpirationSeconds(): number {
  return env.mediaPresignExpirationMinutes * 60;
}
