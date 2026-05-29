import { env } from '../../../config/env.js';
import { MediaUploadError } from '../../../errors/mediaErrors.js';
import { identifyBytesViaMagika } from '../../../magika/magikaHttpClient.js';
import { extensionFromFileName } from '../../../policy/mediaUploadPolicy.js';
import { resolveDetectedMime } from '../../../validation/magic/fileMagic.js';
import { decodeFileHeadBase64 } from '../../../validation/util/decodeFileHead.js';
import type { Filter, PresignMessage } from '../../core/types.js';

/** Content-based MIME via magika-service, or copy sniffed MIME when skip env is set. */
export class MagikaIdentifyOrSkipTransformer implements Filter<PresignMessage> {
  readonly name = 'MagikaIdentifyOrSkipTransformer';
  readonly kind = 'transformer' as const;

  async process(msg: PresignMessage): Promise<void> {
    if (env.mediaMagikaSkipOnMagicMatch && msg.sniffedMime) {
      msg.detectedMime = msg.sniffedMime;
      return;
    }

    const ext = extensionFromFileName(msg.fileName);
    const buffer = msg.fileHeadBuffer ?? decodeFileHeadBase64(msg.fileHeadBase64);

    let magikaResult: { label: string; mime: string };
    try {
      magikaResult = await identifyBytesViaMagika(buffer);
    } catch (err) {
      if (err instanceof MediaUploadError) {
        throw err;
      }
      const message = err instanceof Error ? err.message : 'Magika detection failed';
      throw new MediaUploadError('UNSUPPORTED_CONTENT_TYPE', message, msg.strategy.maxBytes);
    }

    msg.detectedMime = resolveDetectedMime(ext, buffer, magikaResult);
  }
}
