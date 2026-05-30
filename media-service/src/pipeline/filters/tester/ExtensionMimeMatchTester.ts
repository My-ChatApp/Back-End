import { extensionFromFileName } from '../../../policy/mediaUploadPolicy.js';
import { assertExtensionMatchesMime } from '../../../validation/magic/fileMagic.js';
import type { Filter, PresignMessage } from '../../core/types.js';

export class ExtensionMimeMatchTester implements Filter<PresignMessage> {
  readonly name = 'ExtensionMimeMatchTester';
  readonly kind = 'tester' as const;

  async process(msg: PresignMessage): Promise<void> {
    const mime = msg.detectedMime;
    if (!mime) {
      throw new Error('ExtensionMimeMatchTester: detectedMime is not set');
    }

    const ext = extensionFromFileName(msg.fileName);
    assertExtensionMatchesMime(ext, mime, msg.strategy.maxBytes);
  }
}
