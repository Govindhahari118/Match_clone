export const GENERIC_GOVERNMENT_ID_LEVEL = 2;

/**
 * Manual review of a generic government ID has one deterministic evidence strength.
 * It may establish level 2, but must never downgrade a stronger provider-issued verification.
 * Rejection of a new request does not erase previously established verification evidence.
 */
export function verificationLevelAfterGenericReview(
  previousLevel: unknown,
  approved: boolean
): number {
  const numeric = Number(previousLevel);
  const safePrevious = Number.isFinite(numeric)
    ? Math.max(0, Math.min(5, Math.trunc(numeric)))
    : 0;
  return approved
    ? Math.max(safePrevious, GENERIC_GOVERNMENT_ID_LEVEL)
    : safePrevious;
}
