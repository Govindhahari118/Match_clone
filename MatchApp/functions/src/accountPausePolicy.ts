const USER_PAUSE_STATUSES = new Set(["ACTIVE", "PAUSED"]);
const ENFORCED_NON_ACTIVE = new Set(["UNDER_REVIEW", "RESTRICTED", "SUSPENDED"]);

export type UserPauseDecision =
  | { allowed: true; nextStatus: "ACTIVE" | "PAUSED"; paused: boolean }
  | { allowed: false; reason: "DELETION" | "ENFORCEMENT" | "INVALID_STATE" };

/**
 * User-controlled pause/resume must never override an operational enforcement state.
 *
 * The enforcement document is checked independently from the public accountStatus so a stale or
 * partially repaired public document cannot become an escape hatch from moderation.
 */
export function userPauseTransition(
  currentStatusValue: unknown,
  enforcementStatusValue: unknown,
  requestedPaused: boolean
): UserPauseDecision {
  const current = String(currentStatusValue || "ACTIVE").trim().toUpperCase();
  const enforcement = String(enforcementStatusValue || "ACTIVE").trim().toUpperCase();

  if (current === "DELETING" || current === "DELETED") {
    return { allowed: false, reason: "DELETION" };
  }
  if (ENFORCED_NON_ACTIVE.has(enforcement) || ENFORCED_NON_ACTIVE.has(current)) {
    return { allowed: false, reason: "ENFORCEMENT" };
  }
  if (enforcement !== "ACTIVE" || !USER_PAUSE_STATUSES.has(current)) {
    return { allowed: false, reason: "INVALID_STATE" };
  }

  return {
    allowed: true,
    nextStatus: requestedPaused ? "PAUSED" : "ACTIVE",
    paused: requestedPaused,
  };
}
