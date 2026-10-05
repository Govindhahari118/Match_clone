export const PROFILE_PHOTO_MIME_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
]);

export const PROFILE_VIDEO_MIME_TYPES = new Set([
  "video/mp4",
]);

export const PROFILE_VOICE_BIO_MIME_TYPES = new Set([
  "audio/mp4",
]);

export const VERIFICATION_DOCUMENT_MIME_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "application/pdf",
]);

function startsWithBytes(value: Buffer, expected: number[]): boolean {
  return value.length >= expected.length &&
    expected.every((byte, index) => value[index] === byte);
}

function isJpeg(value: Buffer): boolean {
  return startsWithBytes(value, [0xff, 0xd8, 0xff]);
}

function isPng(value: Buffer): boolean {
  return startsWithBytes(value, [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
}

function isWebp(value: Buffer): boolean {
  return value.length >= 12 &&
    value.subarray(0, 4).toString("ascii") === "RIFF" &&
    value.subarray(8, 12).toString("ascii") === "WEBP";
}

function isPdf(value: Buffer): boolean {
  return value.length >= 5 && value.subarray(0, 5).toString("ascii") === "%PDF-";
}

function isMp4(value: Buffer): boolean {
  // ISO Base Media File Format: size (4 bytes), then the ftyp box marker.
  return value.length >= 12 && value.subarray(4, 8).toString("ascii") === "ftyp";
}

export function fileSignatureMatchesMime(prefix: Buffer, mime: string): boolean {
  switch (mime.toLowerCase()) {
    case "image/jpeg": return isJpeg(prefix);
    case "image/png": return isPng(prefix);
    case "image/webp": return isWebp(prefix);
    case "application/pdf": return isPdf(prefix);
    case "video/mp4":
    case "audio/mp4":
      return isMp4(prefix);
    default: return false;
  }
}
