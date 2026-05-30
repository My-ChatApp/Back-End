import { sniffMimeFromBuffer } from '../../../validation/magic/fileMagic.js';
import type { Filter, PresignMessage } from '../../core/types.js';

export class MagicSniffTransformer implements Filter<PresignMessage> {
  readonly name = 'MagicSniffTransformer';
  readonly kind = 'transformer' as const;

  async process(msg: PresignMessage): Promise<void> {
    const buffer = msg.fileHeadBuffer;
    if (!buffer) {
      return;
    }

    const sniffed = sniffMimeFromBuffer(buffer);
    if (sniffed !== null) {
      msg.sniffedMime = sniffed;
    }
  }
}
