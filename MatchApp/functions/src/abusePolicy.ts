export const MAX_DAILY_INTERESTS_SAFETY = 200;
export const HIGH_VOLUME_INTEREST_SIGNAL_THRESHOLD = 50;
export const HIGH_VOLUME_MESSAGE_SIGNAL_THRESHOLD = 300;
export const MAX_DAILY_REPORTS = 25;
export const MAX_DAILY_SUPPORT_TICKETS = 10;

export function safeUsageCount(value: unknown): number {
  const numeric = Number(value || 0);
  if (!Number.isFinite(numeric) || numeric <= 0) return 0;
  return Math.max(0, Math.trunc(numeric));
}

export function usageAllowed(current: unknown, limit: number): boolean {
  return safeUsageCount(current) < Math.max(1, Math.trunc(limit));
}

export function crossesThreshold(
  current: unknown,
  threshold: number
): boolean {
  const before = safeUsageCount(current);
  const next = before + 1;
  return before < threshold && next >= threshold;
}
