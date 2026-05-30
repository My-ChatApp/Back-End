import { MediaUploadError } from '../../../errors/mediaErrors.js';
import { extensionFromFileName } from '../../../policy/mediaUploadPolicy.js';
import type { Filter, PresignMessage } from '../../core/types.js';

export class ExtensionAllowlistTester implements Filter<PresignMessage> {
  readonly name = 'ExtensionAllowlistTester';
  readonly kind = 'tester' as const;

  async process(msg: PresignMessage): Promise<void> {
    const ext = extensionFromFileName(msg.fileName);
    if (!msg.strategy.isExtensionAllowed(ext)) {
      throw new MediaUploadError(
        'UNSUPPORTED_EXTENSION',
        `File extension not allowed: ${ext || '(none)'}`,
        msg.strategy.maxBytes
      );
    }
  }
}
