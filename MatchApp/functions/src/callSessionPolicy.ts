export type CallKind = "VOICE" | "VIDEO";
export type CallSessionState =
  | "REQUESTED"
  | "RINGING"
  | "CONNECTED"
  | "REJECTED"
  | "MISSED"
  | "CANCELLED"
  | "ENDED"
  | "FAILED";

const TERMINAL = new Set<CallSessionState>([
  "REJECTED",
  "MISSED",
  "CANCELLED",
  "ENDED",
  "FAILED",
]);

const ALLOWED: Record<CallSessionState, ReadonlySet<CallSessionState>> = {
  REQUESTED: new Set(["RINGING", "REJECTED", "CANCELLED", "FAILED"]),
  RINGING: new Set(["CONNECTED", "REJECTED", "MISSED", "CANCELLED", "FAILED"]),
  CONNECTED: new Set(["ENDED", "FAILED"]),
  REJECTED: new Set(),
  MISSED: new Set(),
  CANCELLED: new Set(),
  ENDED: new Set(),
  FAILED: new Set(),
};

export function normalizeCallKind(value: unknown): CallKind {
  return String(value || "").trim().toUpperCase() === "VIDEO" ? "VIDEO" : "VOICE";
}

export function canTransitionCallState(
  from: CallSessionState,
  to: CallSessionState
): boolean {
  if (from === to) return true;
  if (TERMINAL.has(from)) return false;
  return ALLOWED[from].has(to);
}

export function callInitiationAllowed(
  recentAccountStarts: number,
  recentPairStarts: number,
  accountLimit: number,
  pairLimit: number
): boolean {
  return recentAccountStarts < accountLimit && recentPairStarts < pairLimit;
}
