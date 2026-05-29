import type { Filter, PresignMessage } from '../../core/types.js';

/** Termination point: ensures the pipeline produced a presign response. */
export class PresignResponseConsumer implements Filter<PresignMessage> {
  readonly name = 'PresignResponseConsumer';
  readonly kind = 'consumer' as const;

  async process(msg: PresignMessage): Promise<void> {
    if (!msg.response) {
      throw new Error('PresignResponseConsumer: pipeline did not produce a response');
    }
  }
}
