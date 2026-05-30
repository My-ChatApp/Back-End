import { MediaUploadError } from '../../../errors/mediaErrors.js';
import { extensionFromFileName } from '../../../policy/mediaUploadPolicy.js';
import { mimeMatchesExtension } from '../../../validation/magic/fileMagic.js';
import type { Filter, PresignMessage } from '../../core/types.js';

export class ExtensionMagicMatchTester implements Filter<PresignMessage> {
  readonly name = 'ExtensionMagicMatchTester';
  readonly kind = 'tester' as const;

  async process(msg: PresignMessage): Promise<void> {
    const sniffed = msg.sniffedMime;
    if (sniffed === undefined) {
      return;
    }

    const ext = extensionFromFileName(msg.fileName);
    if (!mimeMatchesExtension(sniffed, ext)) {
      throw new MediaUploadError(
        'CONTENT_TYPE_MISMATCH',
        `File content (${sniffed}) does not match extension (${ext || '(none)'})`,
        msg.strategy.maxBytes
      );
    }
  }
}
