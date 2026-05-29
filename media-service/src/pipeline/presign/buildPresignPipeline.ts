import { Pipeline } from '../core/Pipeline.js';
import type { PresignMessage } from '../core/types.js';
import { PresignResponseConsumer } from '../filters/consumer/PresignResponseConsumer.js';
import { PresignInputProducer } from '../filters/producer/PresignInputProducer.js';
import { AttachUploadStrategyTransformer } from '../filters/transformer/AttachUploadStrategyTransformer.js';
import { BuildS3ObjectKeyTransformer } from '../filters/transformer/BuildS3ObjectKeyTransformer.js';
import { DecodeFileHeadTransformer } from '../filters/transformer/DecodeFileHeadTransformer.js';
import { MagikaIdentifyOrSkipTransformer } from '../filters/transformer/MagikaIdentifyOrSkipTransformer.js';
import { MagicSniffTransformer } from '../filters/transformer/MagicSniffTransformer.js';
import { S3PresignTransformer } from '../filters/transformer/S3PresignTransformer.js';
import { ExtensionAllowlistTester } from '../filters/tester/ExtensionAllowlistTester.js';
import { ExtensionMagicMatchTester } from '../filters/tester/ExtensionMagicMatchTester.js';
import { ExtensionMimeMatchTester } from '../filters/tester/ExtensionMimeMatchTester.js';
import { MediaConfigReadyTester } from '../filters/tester/MediaConfigReadyTester.js';
import { MimeAllowlistTester } from '../filters/tester/MimeAllowlistTester.js';
import { SizeLimitTester } from '../filters/tester/SizeLimitTester.js';

export function buildPresignPipeline(): Pipeline<PresignMessage> {
  return new Pipeline([
    new PresignInputProducer(),
    new AttachUploadStrategyTransformer(),
    new MediaConfigReadyTester(),
    new SizeLimitTester(),
    new ExtensionAllowlistTester(),
    new DecodeFileHeadTransformer(),
    new MagicSniffTransformer(),
    new ExtensionMagicMatchTester(),
    new MagikaIdentifyOrSkipTransformer(),
    new MimeAllowlistTester(),
    new ExtensionMimeMatchTester(),
    new BuildS3ObjectKeyTransformer(),
    new S3PresignTransformer(),
    new PresignResponseConsumer(),
  ]);
}
