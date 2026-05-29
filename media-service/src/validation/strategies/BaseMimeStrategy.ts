import type { MimeAllowlistStrategy } from './MimeAllowlistStrategy.js';

export abstract class BaseMimeStrategy implements MimeAllowlistStrategy {
  abstract readonly purpose: string;
  abstract readonly maxBytes: number;
  abstract readonly allowedExtensions: ReadonlySet<string>;
  abstract readonly allowedMimeTypes: ReadonlySet<string>;

  isExtensionAllowed(extension: string): boolean {
    if (!extension) {
      return false;
    }
    return this.allowedExtensions.has(extension.toLowerCase());
  }

  isMimeAllowed(mime: string): boolean {
    return this.allowedMimeTypes.has(mime.toLowerCase());
  }
}
