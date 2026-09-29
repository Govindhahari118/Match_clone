export const DATA_EXPORT_COOLDOWN_MS = 10 * 60 * 1000;

export function dataExportRetryAfterMs(
  nextAllowedAtMillis: unknown,
  nowMillis: number
): number {
  const next = Number(nextAllowedAtMillis || 0);
  if (!Number.isFinite(next) || !Number.isFinite(nowMillis)) return 0;
  return Math.max(0, Math.trunc(next - nowMillis));
}
