import type { MimeAllowlistStrategy } from '../../validation/strategies/MimeAllowlistStrategy.js';

export type FilterKind = 'producer' | 'transformer' | 'tester' | 'consumer';

export interface Filter<T> {
  readonly name: string;
  readonly kind: FilterKind;
  process(msg: T): Promise<void>;
}

export interface PresignedUploadResponse {
  uploadUrl: string;
  publicUrl: string;
  key: string;
  /** Validated MIME — client must use this on S3 PUT (matches presigned signature). */
  contentType: string;
}

/** Message carried on the presign pipe (point-to-point, in-process). */
export interface PresignMessage {
  userId: string;
  purpose: string;
  normalizedPurpose: string;
  fileName: string;
  contentType: string;
  contentLength: number;
  fileHeadBase64: string;
  strategy: MimeAllowlistStrategy;
  fileHeadBuffer?: Buffer;
  sniffedMime?: string;
  detectedMime?: string;
  objectKey?: string;
  response?: PresignedUploadResponse;
}

export interface PresignPipelineInput {
  userId: string;
  purpose: string;
  fileName: string;
  contentType: string;
  contentLength: number;
  fileHeadBase64: string;
}
