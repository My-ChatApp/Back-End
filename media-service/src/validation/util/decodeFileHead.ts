import { mediaUploadConfig } from '../../config/mediaUploadConfig.js';
import { MediaUploadError } from '../../errors/mediaErrors.js';

const MIN_SAMPLE_BYTES = 16;

export function decodeFileHeadBase64(base64: string): Buffer {
  let normalized = base64.trim();
  const dataUrlMatch = /^data:[^;]+;base64,(.+)$/i.exec(normalized);
  if (dataUrlMatch) {
    normalized = dataUrlMatch[1];
  }

  let buffer: Buffer;
  try {
    buffer = Buffer.from(normalized, 'base64');
  } catch {
    throw new MediaUploadError('INVALID_REQUEST', 'fileHeadBase64 is not valid base64');
  }

  if (buffer.length < MIN_SAMPLE_BYTES) {
    throw new MediaUploadError(
      'INVALID_REQUEST',
      `fileHeadBase64 must decode to at least ${MIN_SAMPLE_BYTES} bytes`
    );
  }

  const maxSample = mediaUploadConfig.fileHeadMaxBytes;
  if (buffer.length > maxSample) {
    throw new MediaUploadError(
      'INVALID_REQUEST',
      `fileHeadBase64 exceeds max sample size (${maxSample} bytes)`
    );
  }

  return buffer;
}
