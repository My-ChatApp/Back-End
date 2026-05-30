import { MediaUploadError } from '../../../errors/mediaErrors.js';
import type { Filter, PresignMessage } from '../../core/types.js';

export class SizeLimitTester implements Filter<PresignMessage> {
  readonly name = 'SizeLimitTester';
  readonly kind = 'tester' as const;

  async process(msg: PresignMessage): Promise<void> {
    const { contentLength, strategy } = msg;
    const maxBytes = strategy.maxBytes;

    if (contentLength <= 0) {
      throw new MediaUploadError('INVALID_REQUEST', 'contentLength must be positive', maxBytes);
    }

    if (contentLength > maxBytes) {
      throw new MediaUploadError('FILE_TOO_LARGE', 'File exceeds size limit', maxBytes);
    }
  }
}
