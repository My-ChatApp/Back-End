export interface MediaErrorBody {
  code: string;
  message: string;
  maxBytes?: number;
}

export class MediaUploadError extends Error {
  readonly code: string;
  readonly maxBytes?: number;

  constructor(code: string, message: string, maxBytes?: number) {
    super(message);
    this.name = 'MediaUploadError';
    this.code = code;
    this.maxBytes = maxBytes;
  }

  toBody(): MediaErrorBody {
    const body: MediaErrorBody = { code: this.code, message: this.message };
    if (this.maxBytes !== undefined) {
      body.maxBytes = this.maxBytes;
    }
    return body;
  }
}

export function httpStatusForMediaCode(code: string): number {
  switch (code) {
    case 'FILE_TOO_LARGE':
    case 'UNSUPPORTED_CONTENT_TYPE':
    case 'UNSUPPORTED_EXTENSION':
    case 'CONTENT_TYPE_MISMATCH':
    case 'INVALID_REQUEST':
      return 400;
    case 'CONFIG_ERROR':
      return 503;
    case 'RATE_LIMIT_EXCEEDED':
      return 429;
    default:
      return 400;
  }
}
