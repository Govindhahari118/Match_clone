export const REPORT_REASON_CODES = [
  "FAKE_PROFILE",
  "ALREADY_MARRIED",
  "SCAM_OR_MONEY_REQUEST",
  "HARASSMENT",
  "INAPPROPRIATE_CONTENT",
  "WRONG_INFORMATION",
  "STOLEN_PHOTO",
  "UNDERAGE_CONCERN",
  "SPAM",
  "OTHER",
] as const;

export type ReportReasonCode = typeof REPORT_REASON_CODES[number];

const LEGACY_REASON_MAP: Record<string, ReportReasonCode> = {
  "Fake identity": "FAKE_PROFILE",
  "Fake profile": "FAKE_PROFILE",
  "Already married": "ALREADY_MARRIED",
  "Scam or money request": "SCAM_OR_MONEY_REQUEST",
  "Spam or scam": "SCAM_OR_MONEY_REQUEST",
  "Harassment": "HARASSMENT",
  "Offensive content": "INAPPROPRIATE_CONTENT",
  "Inappropriate content": "INAPPROPRIATE_CONTENT",
  "Wrong information": "WRONG_INFORMATION",
  "Stolen photo": "STOLEN_PHOTO",
  "Underage concern": "UNDERAGE_CONCERN",
  "Under age": "UNDERAGE_CONCERN",
  "Spam": "SPAM",
  "Other": "OTHER",
};

export function normalizeReportReason(value: unknown): ReportReasonCode | null {
  const raw = typeof value === "string" ? value.trim() : "";
  if (!raw) return null;
  if ((REPORT_REASON_CODES as readonly string[]).includes(raw)) {
    return raw as ReportReasonCode;
  }
  return LEGACY_REASON_MAP[raw] || null;
}
