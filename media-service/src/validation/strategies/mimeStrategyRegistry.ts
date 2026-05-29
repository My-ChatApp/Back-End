import { mediaUploadConfig } from '../../config/mediaUploadConfig.js';
import { MediaUploadError } from '../../errors/mediaErrors.js';
import type { MimeAllowlistStrategy } from './MimeAllowlistStrategy.js';
import { ConfiguredMimeStrategy } from './ConfiguredMimeStrategy.js';

const strategies: MimeAllowlistStrategy[] = Object.entries(mediaUploadConfig.purposes).map(
  ([purpose, config]) => new ConfiguredMimeStrategy(purpose, config)
);

const byPurpose = new Map(strategies.map((s) => [s.purpose, s]));

export function getMimeStrategy(purpose: string): MimeAllowlistStrategy {
  const normalized = purpose.trim().toLowerCase();
  const strategy = byPurpose.get(normalized);
  if (!strategy) {
    throw new MediaUploadError('INVALID_REQUEST', `Unknown purpose: ${purpose}`);
  }
  return strategy;
}
