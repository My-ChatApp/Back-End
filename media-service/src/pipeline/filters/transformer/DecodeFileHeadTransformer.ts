import { decodeFileHeadBase64 } from '../../../validation/util/decodeFileHead.js';
import type { Filter, PresignMessage } from '../../core/types.js';

export class DecodeFileHeadTransformer implements Filter<PresignMessage> {
  readonly name = 'DecodeFileHeadTransformer';
  readonly kind = 'transformer' as const;

  async process(msg: PresignMessage): Promise<void> {
    msg.fileHeadBuffer = decodeFileHeadBase64(msg.fileHeadBase64);
  }
}
