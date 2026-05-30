import { S3Client } from '@aws-sdk/client-s3';
import { fromNodeProviderChain } from '@aws-sdk/credential-providers';
import { env } from '../config/env.js';

function resolveCredentials() {
  const { awsAccessKeyId, awsSecretAccessKey } = env;
  if (
    awsAccessKeyId &&
    awsSecretAccessKey &&
    awsAccessKeyId !== 'local'
  ) {
    return {
      accessKeyId: awsAccessKeyId,
      secretAccessKey: awsSecretAccessKey,
    };
  }
  return fromNodeProviderChain();
}

export const s3Client = new S3Client({
  region: env.awsRegion,
  credentials: resolveCredentials(),
  maxAttempts: env.awsSdkMaxAttempts,
});
