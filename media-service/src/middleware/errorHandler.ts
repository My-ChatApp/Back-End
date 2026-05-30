import type { NextFunction, Request, Response } from 'express';
import { ZodError } from 'zod';
import { httpStatusForMediaCode, MediaUploadError } from '../errors/mediaErrors.js';

export function errorHandler(
  err: unknown,
  _req: Request,
  res: Response,
  _next: NextFunction
): void {
  if (err instanceof MediaUploadError) {
    res.status(httpStatusForMediaCode(err.code)).json(err.toBody());
    return;
  }

  if (err instanceof ZodError) {
    res.status(400).json({
      code: 'INVALID_REQUEST',
      message: 'Invalid request body',
    });
    return;
  }

  const message = err instanceof Error ? err.message : 'Unexpected error';
  res.status(500).json({
    code: 'INTERNAL_ERROR',
    message: message || 'Unexpected error',
  });
}
