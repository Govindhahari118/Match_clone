export const QUIET_HOURS_START = 22;
export const QUIET_HOURS_END = 7;

function localHour(now: Date, timeZone: string): number | null {
  const zone = typeof timeZone === "string" ? timeZone.trim().slice(0, 64) : "";
  if (!zone || Number.isNaN(now.getTime())) return null;
  try {
    const parts = new Intl.DateTimeFormat("en-US", {
      timeZone: zone,
      hour: "2-digit",
      hourCycle: "h23",
    }).formatToParts(now);
    const hour = Number(parts.find((part) => part.type === "hour")?.value);
    return Number.isInteger(hour) && hour >= 0 && hour <= 23 ? hour : null;
  } catch {
    return null;
  }
}

export function normalizeQuietHour(value: unknown, fallback: number): number {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 0 || parsed > 23) return fallback;
  return parsed;
}

/**
 * Quiet hours are a push-delivery preference only. Durable in-app notifications are persisted
 * regardless, and security/verification critical notices bypass this preference.
 *
 * The member's timezone is supplied by the Android foreground lifecycle. Start/end are optional
 * product-configurable hours, clamped to valid clock values; equal start/end deliberately means
 * "no quiet window" rather than muting every optional notification for 24 hours.
 */
export function quietHoursActive(
  enabled: boolean,
  timeZone: string,
  now: Date = new Date(),
  startHour = QUIET_HOURS_START,
  endHour = QUIET_HOURS_END
): boolean {
  if (!enabled) return false;
  const hour = localHour(now, timeZone);
  if (hour == null) return false;

  const start = normalizeQuietHour(startHour, QUIET_HOURS_START);
  const end = normalizeQuietHour(endHour, QUIET_HOURS_END);
  if (start === end) return false;

  return start < end
    ? hour >= start && hour < end
    : hour >= start || hour < end;
}
