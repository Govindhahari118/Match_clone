export const QUIET_HOURS_START = 22;
export const QUIET_HOURS_END = 7;

function localHour(now: Date, timeZone: string): number | null {
  const zone = typeof timeZone === "string" ? timeZone.trim().slice(0, 64) : "";
  if (!zone) return null;
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

/**
 * Quiet hours are deliberately a push-delivery preference only. The durable in-app notification
 * is still persisted, and security/verification critical notices bypass this preference.
 */
export function quietHoursActive(
  enabled: boolean,
  timeZone: string,
  now: Date = new Date()
): boolean {
  if (!enabled) return false;
  const hour = localHour(now, timeZone);
  if (hour == null) return false;
  return hour >= QUIET_HOURS_START || hour < QUIET_HOURS_END;
}
