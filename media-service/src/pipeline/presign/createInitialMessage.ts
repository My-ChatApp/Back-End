import { getMimeStrategy } from '../../validation/strategies/mimeStrategyRegistry.js';
import type { PresignMessage, PresignPipelineInput } from '../core/types.js';

/** Producer (outside pipeline): builds the initial message from HTTP/API input. */
export function createInitialMessage(input: PresignPipelineInput): PresignMessage {
  const normalizedPurpose = input.purpose.trim().toLowerCase();
  return {
    userId: input.userId,
    purpose: input.purpose,
    normalizedPurpose,
    fileName: input.fileName,
    contentType: input.contentType,
    contentLength: input.contentLength,
    fileHeadBase64: input.fileHeadBase64,
    strategy: getMimeStrategy(normalizedPurpose),
  };
}
