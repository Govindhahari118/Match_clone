export const PHOTO_REQUEST_COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000;

export function photoRequestCoolingDown(
  lastRequestedAtMillis: number,
  nowMillis: number
): boolean {
  if (!Number.isFinite(lastRequestedAtMillis) || lastRequestedAtMillis <= 0) return false;
  if (!Number.isFinite(nowMillis) || nowMillis <= 0) return true;
  return nowMillis - lastRequestedAtMillis < PHOTO_REQUEST_COOLDOWN_MS;
}

export function photoRequestNextAllowedAt(
  lastRequestedAtMillis: number
): number {
  if (!Number.isFinite(lastRequestedAtMillis) || lastRequestedAtMillis <= 0) return 0;
  return lastRequestedAtMillis + PHOTO_REQUEST_COOLDOWN_MS;
}
