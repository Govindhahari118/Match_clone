export const GENERIC_GOVERNMENT_ID_LEVEL = 2;

export function verificationLevelForMethod(method: unknown): number | null {
  return method === "GENERIC_GOVERNMENT_ID_REVIEW"
    ? GENERIC_GOVERNMENT_ID_LEVEL
    : null;
}

/**
 * Higher levels represent distinct evidence and must never be selected manually while their
 * corresponding evidence/provider flow is absent.
 */
export function reviewerRequestedLevelIsValid(method: unknown, requested: unknown): boolean {
  const level = verificationLevelForMethod(method);
  if (level == null) return false;
  if (requested == null) return true;
  const numeric = Number(requested);
  return Number.isInteger(numeric) && numeric === level;
}
