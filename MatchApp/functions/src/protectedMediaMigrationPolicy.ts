type ProtectedMediaField = "photoUrl" | "videoUrl" | "voiceBioUrl";

const ROOT_BY_FIELD: Record<ProtectedMediaField, string> = {
  photoUrl: "photos",
  videoUrl: "videos",
  voiceBioUrl: "voicebios",
};

function safeDecode(value: string): string | null {
  try {
    return decodeURIComponent(value);
  } catch {
    return null;
  }
}

/**
 * Converts only Firebase/Google Storage URLs that resolve to the expected owner-scoped media root.
 * Unknown hosts, cross-owner paths and malformed values are deliberately left untouched.
 */
export function protectedObjectIdentity(
  value: unknown,
  uid: string,
  field: ProtectedMediaField
): string | null {
  if (typeof value !== "string" || !uid) return null;
  const trimmed = value.trim();
  if (!trimmed.startsWith("https://")) return null;

  let parsed: URL;
  try {
    parsed = new URL(trimmed);
  } catch {
    return null;
  }

  let bucket = "";
  let objectPath = "";
  if (parsed.hostname === "firebasestorage.googleapis.com") {
    const parts = parsed.pathname.split("/").filter(Boolean);
    const bucketIndex = parts.indexOf("b");
    const objectIndex = parts.indexOf("o");
    if (
      bucketIndex < 0 ||
      objectIndex < 0 ||
      bucketIndex + 1 >= parts.length ||
      objectIndex + 1 >= parts.length
    ) return null;
    bucket = parts[bucketIndex + 1];
    const decoded = safeDecode(parts.slice(objectIndex + 1).join("/"));
    if (!decoded) return null;
    objectPath = decoded;
  } else if (parsed.hostname === "storage.googleapis.com") {
    const parts = parsed.pathname.split("/").filter(Boolean);
    if (parts.length < 2) return null;
    bucket = parts[0];
    const decoded = safeDecode(parts.slice(1).join("/"));
    if (!decoded) return null;
    objectPath = decoded;
  } else {
    return null;
  }

  const expectedPrefix = `${ROOT_BY_FIELD[field]}/${uid}/`;
  if (!bucket || !objectPath.startsWith(expectedPrefix)) return null;
  if (objectPath.includes("..")) return null;
  return `gs://${bucket}/${objectPath}`;
}
