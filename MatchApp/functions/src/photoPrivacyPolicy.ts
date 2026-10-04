export type ProfilePhotoVisibility = "PUBLIC" | "ACCEPTED_ONLY" | "HIDDEN";

export function normalizeProfilePhotoVisibility(value: unknown): ProfilePhotoVisibility {
  const normalized = typeof value === "string" ? value.trim().toUpperCase() : "";
  if (normalized === "ACCEPTED_ONLY" || normalized === "HIDDEN") return normalized;
  return "PUBLIC";
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
