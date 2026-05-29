import { mediaUploadConfig } from '../../config/mediaUploadConfig.js';
import { MediaUploadError } from '../../errors/mediaErrors.js';

export function mimeMatchesExtension(mime: string, extension: string): boolean {
  const ext = extension.toLowerCase();
  const allowed = mediaUploadConfig.extensionToMimes[ext];
  if (!allowed) {
    return false;
  }
  return allowed.includes(mime.toLowerCase());
}

function readAscii(buf: Buffer, offset: number, length: number): string {
  return buf.subarray(offset, offset + length).toString('ascii');
}

/** Fast magic-byte sniff (~0ms). Returns null if unknown / ambiguous. */
export function sniffMimeFromBuffer(buf: Buffer): string | null {
  if (buf.length >= 8 && buf[0] === 0x89 && readAscii(buf, 1, 3) === 'PNG') {
    return pickMimeIfAllowed('.png', 'image/png');
  }
  if (buf.length >= 3 && buf[0] === 0xff && buf[1] === 0xd8 && buf[2] === 0xff) {
    return pickMimeIfAllowed('.jpg', 'image/jpeg');
  }
  if (buf.length >= 6 && (readAscii(buf, 0, 6) === 'GIF87a' || readAscii(buf, 0, 6) === 'GIF89a')) {
    return pickMimeIfAllowed('.gif', 'image/gif');
  }
  if (buf.length >= 12 && readAscii(buf, 0, 4) === 'RIFF' && readAscii(buf, 8, 4) === 'WEBP') {
    return pickMimeIfAllowed('.webp', 'image/webp');
  }
  if (buf.length >= 4 && readAscii(buf, 0, 4) === 'OggS') {
    return pickMimeIfAllowed('.ogg', 'audio/ogg');
  }
  if (buf.length >= 4 && readAscii(buf, 0, 4) === '%PDF') {
    return pickMimeIfAllowed('.pdf', 'application/pdf');
  }
  if (buf.length >= 4 && readAscii(buf, 0, 4) === 'ID3') {
    return pickMimeIfAllowed('.mp3', 'audio/mpeg');
  }
  if (buf.length >= 2 && buf[0] === 0xff && (buf[1] & 0xe0) === 0xe0) {
    return pickMimeIfAllowed('.mp3', 'audio/mpeg');
  }
  if (buf.length >= 12 && readAscii(buf, 4, 4) === 'ftyp') {
    const brand = readAscii(buf, 8, 4).toLowerCase();
    if (brand.startsWith('m4a')) {
      return pickMimeIfAllowed('.m4a', 'audio/mp4');
    }
    return pickMimeIfAllowed('.mp4', 'video/mp4');
  }
  if (buf.length >= 4 && buf[0] === 0x1a && buf[1] === 0x45 && buf[2] === 0xdf && buf[3] === 0xa3) {
    return pickMimeIfAllowed('.webm', 'video/webm');
  }
  if (buf.length >= 8 && buf[0] === 0xd0 && buf[1] === 0xcf && buf[2] === 0x11 && buf[3] === 0xe0) {
    return pickMimeIfAllowed('.doc', 'application/msword');
  }

  return null;
}

function pickMimeIfAllowed(extension: string, mime: string): string | null {
  const allowed = mediaUploadConfig.extensionToMimes[extension.toLowerCase()];
  if (!allowed?.includes(mime)) {
    return null;
  }
  return mime;
}

export function resolveDetectedMime(
  extension: string,
  buffer: Buffer,
  magika?: { label: string; mime: string }
): string {
  const sniffed = sniffMimeFromBuffer(buffer);
  if (sniffed && mimeMatchesExtension(sniffed, extension)) {
    return sniffed;
  }

  if (magika) {
    const labelKey = magika.label.trim().toLowerCase();
    const fromLabel = mediaUploadConfig.magikaLabelToMime[labelKey];
    if (fromLabel && mimeMatchesExtension(fromLabel, extension)) {
      return fromLabel;
    }
    if (mimeMatchesExtension(magika.mime, extension)) {
      return magika.mime;
    }
  }

  if (sniffed) {
    return sniffed;
  }

  return magika?.mime ?? 'application/octet-stream';
}

export function assertExtensionMatchesMime(
  extension: string,
  mime: string,
  maxBytes?: number
): void {
  if (!mimeMatchesExtension(mime, extension)) {
    throw new MediaUploadError(
      'CONTENT_TYPE_MISMATCH',
      `File content (${mime}) does not match extension (${extension || '(none)'})`,
      maxBytes
    );
  }
}
