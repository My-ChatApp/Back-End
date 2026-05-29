import { Router } from 'express';
import { z } from 'zod';
import { createPresignedUpload } from '../services/s3Presign.service.js';

const presignedUploadSchema = z.object({
  purpose: z.string().min(1),
  fileName: z.string().optional(),
  contentType: z.string().min(1),
  contentLength: z.number().int().positive(),
  fileHeadBase64: z.string().min(1),
});

export const mediaRouter = Router();

mediaRouter.post('/presigned-upload', async (req, res, next) => {
  try {
    const userId = res.locals.mediaUser?.userId;
    if (!userId) {
      res.status(401).json({
        code: 'INVALID_REQUEST',
        message: 'User context is required',
      });
      return;
    }

    const body = presignedUploadSchema.parse(req.body);
    const fileName = body.fileName ?? 'file';

    const result = await createPresignedUpload(
      userId,
      body.purpose,
      fileName,
      body.contentType,
      body.contentLength,
      body.fileHeadBase64
    );

    res.status(200).json(result);
  } catch (err) {
    next(err);
  }
});
