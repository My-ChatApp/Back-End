import { getMimeStrategy } from '../../../validation/strategies/mimeStrategyRegistry.js';
import type { Filter, PresignMessage } from '../../core/types.js';

/** Ensures normalized purpose and upload strategy are attached to the message. */
export class AttachUploadStrategyTransformer implements Filter<PresignMessage> {
  readonly name = 'AttachUploadStrategyTransformer';
  readonly kind = 'transformer' as const;

  async process(msg: PresignMessage): Promise<void> {
    msg.normalizedPurpose = msg.purpose.trim().toLowerCase();
    msg.strategy = getMimeStrategy(msg.normalizedPurpose);
  }
}
