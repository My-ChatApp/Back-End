import { env } from '../../../config/env.js';
import { normalizedPublicBaseUrl } from '../../../config/mediaProperties.js';
import { MediaUploadError } from '../../../errors/mediaErrors.js';
import type { Filter, PresignMessage } from '../../core/types.js';

export class MediaConfigReadyTester implements Filter<PresignMessage> {
  readonly name = 'MediaConfigReadyTester';
  readonly kind = 'tester' as const;

  async process(_msg: PresignMessage): Promise<void> {
    if (!env.s3Bucket) {
      throw new MediaUploadError('CONFIG_ERROR', 'S3 bucket is not configured');
    }
    if (!normalizedPublicBaseUrl()) {
      throw new MediaUploadError('CONFIG_ERROR', 'MEDIA_PUBLIC_BASE_URL is not configured');
    }
  }
}
