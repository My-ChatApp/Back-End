export const PURPOSE_AVATAR = 'avatar';
export const PURPOSE_COVER = 'cover';
export const PURPOSE_MESSAGE = 'message';

export function buildObjectKey(purpose: string, userId: string, fileName: string): string {
  const ext = extensionFromFileName(fileName);
  const id = crypto.randomUUID();

  switch (purpose) {
    case PURPOSE_AVATAR:
      return `users/${userId}/avatar/${id}${ext}`;
    case PURPOSE_COVER:
      return `users/${userId}/cover/${id}${ext}`;
    case PURPOSE_MESSAGE:
      return `chat/inbox/${userId}/${id}${ext}`;
    default:
      throw new Error(`Unknown purpose: ${purpose}`);
  }
}

export function extensionFromFileName(fileName: string | undefined | null): string {
  if (!fileName?.includes('.')) {
    return '';
  }
  const ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
  if (ext.length > 10) {
    return '';
  }
  return ext;
}
