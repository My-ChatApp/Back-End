export interface MimeAllowlistStrategy {
  readonly purpose: string;
  readonly maxBytes: number;
  readonly allowedExtensions: ReadonlySet<string>;
  readonly allowedMimeTypes: ReadonlySet<string>;
  isExtensionAllowed(extension: string): boolean;
  isMimeAllowed(mime: string): boolean;
}
