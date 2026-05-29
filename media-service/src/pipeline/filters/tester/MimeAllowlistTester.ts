import { MediaUploadError } from '../../../errors/mediaErrors.js';
import type { Filter, PresignMessage } from '../../core/types.js';

export class MimeAllowlistTester implements Filter<PresignMessage> {
  readonly name = 'MimeAllowlistTester';
  readonly kind = 'tester' as const;

  async process(msg: PresignMessage): Promise<void> {
    const mime = msg.detectedMime;
    if (!mime) {
      throw new Error('MimeAllowlistTester: detectedMime is not set');
    }

    if (!msg.strategy.isMimeAllowed(mime)) {
      throw new MediaUploadError(
        'UNSUPPORTED_CONTENT_TYPE',
        `Content type not allowed: ${mime}`,
        msg.strategy.maxBytes
      );
    }
  }
}
