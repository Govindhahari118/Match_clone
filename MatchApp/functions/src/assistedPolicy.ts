export const ASSISTED_PLANS = ["Silver RM", "Gold RM", "Platinum RM"] as const;
export type AssistedPlan = typeof ASSISTED_PLANS[number];

export const ASSISTED_STATUSES = [
  "OPEN",
  "ASSIGNED",
  "IN_PROGRESS",
  "WAITING_ON_MEMBER",
  "RESOLVED",
  "CANCELLED",
] as const;
export type AssistedStatus = typeof ASSISTED_STATUSES[number];

export function normalizeAssistedPlan(value: unknown): AssistedPlan | null {
  const plan = typeof value === "string" ? value.trim() : "";
  return ASSISTED_PLANS.includes(plan as AssistedPlan) ? plan as AssistedPlan : null;
}

export function normalizeAssistedStatus(value: unknown): AssistedStatus | null {
  const status = typeof value === "string" ? value.trim().toUpperCase() : "";
  return ASSISTED_STATUSES.includes(status as AssistedStatus)
    ? status as AssistedStatus
    : null;
}

export function assistedStatusTransitionAllowed(
  current: AssistedStatus,
  next: AssistedStatus
): boolean {
  if (current === next) return true;
  const allowed: Record<AssistedStatus, AssistedStatus[]> = {
    OPEN: ["ASSIGNED", "CANCELLED"],
    ASSIGNED: ["IN_PROGRESS", "WAITING_ON_MEMBER", "RESOLVED", "CANCELLED"],
    IN_PROGRESS: ["WAITING_ON_MEMBER", "RESOLVED", "CANCELLED"],
    WAITING_ON_MEMBER: ["IN_PROGRESS", "RESOLVED", "CANCELLED"],
    RESOLVED: [],
    CANCELLED: [],
  };
  return allowed[current].includes(next);
}
