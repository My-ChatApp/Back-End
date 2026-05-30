import type { Filter } from '../../core/types.js';
import type { PresignMessage } from '../../core/types.js';

/** No-op when message is built by createInitialMessage before pipeline.execute. */
export class PresignInputProducer implements Filter<PresignMessage> {
  readonly name = 'PresignInputProducer';
  readonly kind = 'producer' as const;

  async process(_msg: PresignMessage): Promise<void> {}
}
