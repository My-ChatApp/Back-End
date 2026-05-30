import { buildObjectKey } from '../../../policy/mediaUploadPolicy.js';
import type { Filter, PresignMessage } from '../../core/types.js';

export class BuildS3ObjectKeyTransformer implements Filter<PresignMessage> {
  readonly name = 'BuildS3ObjectKeyTransformer';
  readonly kind = 'transformer' as const;

  async process(msg: PresignMessage): Promise<void> {
    msg.objectKey = buildObjectKey(msg.normalizedPurpose, msg.userId, msg.fileName);
  }
}
