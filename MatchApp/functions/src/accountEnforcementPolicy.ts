export const ENFORCEMENT_STATUSES = [
  "ACTIVE",
  "UNDER_REVIEW",
  "RESTRICTED",
  "SUSPENDED",
] as const;

export type EnforcementStatus = typeof ENFORCEMENT_STATUSES[number];

export function normalizeEnforcementStatus(value: unknown): EnforcementStatus | undefined {
  if (typeof value !== "string") return undefined;
  const normalized = value.trim().toUpperCase();
  return (ENFORCEMENT_STATUSES as readonly string[]).includes(normalized)
    ? normalized as EnforcementStatus
    : undefined;
}

export function enforcementSuppressesInteractions(status: EnforcementStatus): boolean {
  return status !== "ACTIVE";
}
