import type { MediaPurposeConfig } from '../../config/mediaUploadConfig.js';
import { BaseMimeStrategy } from './BaseMimeStrategy.js';

export class ConfiguredMimeStrategy extends BaseMimeStrategy {
  readonly purpose: string;
  readonly maxBytes: number;
  readonly allowedExtensions: ReadonlySet<string>;
  readonly allowedMimeTypes: ReadonlySet<string>;

  constructor(purpose: string, config: MediaPurposeConfig) {
    super();
    this.purpose = purpose;
    this.maxBytes = config.maxBytes;
    this.allowedExtensions = new Set(
      config.types.map((t) => t.extension.trim().toLowerCase())
    );
    this.allowedMimeTypes = new Set(config.types.map((t) => t.mime.trim().toLowerCase()));
  }
}
