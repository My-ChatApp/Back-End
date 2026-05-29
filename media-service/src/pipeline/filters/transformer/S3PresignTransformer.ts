import { PutObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { env } from '../../../config/env.js';
import { presignExpirationSeconds, publicUrlForKey } from '../../../config/mediaProperties.js';
import { s3Client } from '../../../services/awsClient.js';
import type { Filter, PresignMessage } from '../../core/types.js';

export class S3PresignTransformer implements Filter<PresignMessage> {
  readonly name = 'S3PresignTransformer';
  readonly kind = 'transformer' as const;

  async process(msg: PresignMessage): Promise<void> {
    const mime = msg.detectedMime;
    const key = msg.objectKey;
    if (!mime || !key) {
      throw new Error('S3PresignTransformer: detectedMime and objectKey are required');
    }

    const command = new PutObjectCommand({
      Bucket: env.s3Bucket,
      Key: key,
      ContentType: mime,
      ContentLength: msg.contentLength,
    });

    const uploadUrl = await getSignedUrl(s3Client, command, {
      expiresIn: presignExpirationSeconds(),
    });

    msg.response = {
      uploadUrl,
      publicUrl: publicUrlForKey(key),
      key,
      contentType: mime,
    };
  }
}
