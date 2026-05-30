import type { PresignPipelineInput, PresignedUploadResponse } from '../core/types.js';
import { buildPresignPipeline } from './buildPresignPipeline.js';
import { createInitialMessage } from './createInitialMessage.js';

const presignPipeline = buildPresignPipeline();

export async function runPresignPipeline(
  input: PresignPipelineInput
): Promise<PresignedUploadResponse> {
  const msg = createInitialMessage(input);
  const result = await presignPipeline.execute(msg);

  if (!result.response) {
    throw new Error('Presign pipeline did not produce a response');
  }

  return result.response;
}
