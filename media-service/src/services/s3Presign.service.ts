import type { PresignedUploadResponse } from '../pipeline/core/types.js';
import { runPresignPipeline } from '../pipeline/presign/runPresignPipeline.js';

export type { PresignedUploadResponse };

export async function createPresignedUpload(
  userId: string,
  purpose: string,
  fileName: string,
  contentType: string,
  contentLength: number,
  fileHeadBase64: string
): Promise<PresignedUploadResponse> {
  return runPresignPipeline({
    userId,
    purpose,
    fileName,
    contentType,
    contentLength,
    fileHeadBase64,
  });
}
