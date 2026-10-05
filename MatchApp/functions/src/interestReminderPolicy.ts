export const INTEREST_REMINDER_INITIAL_DELAY_MS = 72 * 60 * 60 * 1000;
export const INTEREST_REMINDER_COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000;
export const INTEREST_REMINDER_MAX_COUNT = 2;

export function interestReminderAllowed(
  createdAtMillis: number,
  lastReminderAtMillis: number,
  reminderCount: number,
  nowMillis: number
): boolean {
  if (!Number.isFinite(createdAtMillis) || createdAtMillis <= 0) return false;
  if (!Number.isFinite(nowMillis) || nowMillis <= 0) return false;
  if (!Number.isFinite(reminderCount) || reminderCount < 0 ||
      reminderCount >= INTEREST_REMINDER_MAX_COUNT) return false;

  if (reminderCount === 0) {
    return nowMillis - createdAtMillis >= INTEREST_REMINDER_INITIAL_DELAY_MS;
  }

  if (!Number.isFinite(lastReminderAtMillis) || lastReminderAtMillis <= 0) return false;
  return nowMillis - lastReminderAtMillis >= INTEREST_REMINDER_COOLDOWN_MS;
}

export function nextInterestReminderAt(
  createdAtMillis: number,
  lastReminderAtMillis: number,
  reminderCount: number
): number {
  if (reminderCount >= INTEREST_REMINDER_MAX_COUNT) return 0;
  if (reminderCount <= 0) {
    return createdAtMillis > 0 ? createdAtMillis + INTEREST_REMINDER_INITIAL_DELAY_MS : 0;
  }
  return lastReminderAtMillis > 0 ? lastReminderAtMillis + INTEREST_REMINDER_COOLDOWN_MS : 0;
}
