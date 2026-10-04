export const PHOTO_REQUEST_COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000;
export const PHOTO_REQUEST_DAILY_LIMIT = 20;

export function publicPhotoAvailable(photoUrl: unknown): boolean {
  return typeof photoUrl === "string" && photoUrl.trim().length > 0;
}

export function photoRequestCooldownRemaining(
  status: unknown,
  requestedAtMillis: number,
  now: number
): number {
  if (String(status || "").trim().toUpperCase() !== "PENDING") return 0;
  if (!Number.isFinite(requestedAtMillis) || requestedAtMillis <= 0) return 0;
  return Math.max(0, requestedAtMillis + PHOTO_REQUEST_COOLDOWN_MS - now);
}

export function photoRequestDailyAllowed(count: unknown): boolean {
  const parsed = Number(count);
  if (!Number.isFinite(parsed) || parsed < 0) return true;
  return Math.trunc(parsed) < PHOTO_REQUEST_DAILY_LIMIT;
}
