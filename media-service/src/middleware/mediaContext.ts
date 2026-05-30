import type { NextFunction, Request, Response } from 'express';

export interface MediaUser {
  userId: string;
  email?: string;
}

declare global {
  namespace Express {
    interface Locals {
      mediaUser?: MediaUser;
    }
  }
}

/** Trusts X-UserId / X-Email forwarded by api-gateway (same as MediaContextFilter). */
export function mediaContext(req: Request, res: Response, next: NextFunction): void {
  const userId = req.header('X-UserId')?.trim();
  const email = req.header('X-Email')?.trim();

  if (userId) {
    res.locals.mediaUser = { userId, email: email || undefined };
  }

  next();
}
