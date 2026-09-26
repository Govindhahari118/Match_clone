/**
 * Pure account-deletion policy helpers kept separate from Firebase I/O so retry invariants can be
 * unit-tested without an emulator.
 */

export const PUBLIC_PROFILE_PHASE = "PUBLIC_PROFILE";

export function shouldReassertPublicSuppression(completedPhases: Iterable<string>): boolean {
  for (const phase of completedPhases) {
    if (phase === PUBLIC_PROFILE_PHASE) return false;
  }
  return true;
}

/**
 * Deletion queries intentionally operate in bounded batches. This helper is evidence for the
 * >500-record retry requirement and prevents accidental zero/negative batch configuration.
 */
export function deletionBatchCount(totalRecords: number, batchSize: number): number {
  if (!Number.isFinite(totalRecords) || totalRecords <= 0) return 0;
  if (!Number.isFinite(batchSize) || batchSize <= 0) {
    throw new Error("Deletion batch size must be positive");
  }
  return Math.ceil(Math.floor(totalRecords) / Math.floor(batchSize));
}
