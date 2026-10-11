export type ProfilePhotoVisibility = "PUBLIC" | "ACCEPTED_ONLY" | "HIDDEN";

export function normalizeProfilePhotoVisibility(value: unknown): ProfilePhotoVisibility {
  const normalized = typeof value === "string" ? value.trim().toUpperCase() : "";
  if (normalized === "ACCEPTED_ONLY" || normalized === "HIDDEN") return normalized;
  return value == null || normalized === "PUBLIC" ? "PUBLIC" : "HIDDEN";
}

export function canViewPublishedPhoto(
  visibility: ProfilePhotoVisibility,
  mutualInterest: boolean,
  explicitGrant: boolean
): boolean {
  if (visibility === "PUBLIC") return true;
  if (visibility === "HIDDEN") return false;
  return mutualInterest || explicitGrant;
}

export function canRequestPhotoAccess(
  visibility: ProfilePhotoVisibility,
  requesterInterested: boolean,
  mutualInterest: boolean,
  explicitGrant: boolean
): boolean {
  return visibility === "ACCEPTED_ONLY" &&
    requesterInterested &&
    !mutualInterest &&
    !explicitGrant;
}

/**
 * Legacy contact/photo request IDs join two UIDs with underscores and may collide.
 * Validate stored participant identities before exposing request status or overwriting it.
 */
export function relationshipRequestMatchesParticipants(
  value: Record<string, unknown> | undefined,
  requesterUid: string,
  targetUid: string
): boolean {
  return !!value && !!requesterUid && !!targetUid && requesterUid !== targetUid &&
    value.requesterUid === requesterUid && value.targetUid === targetUid;
}
