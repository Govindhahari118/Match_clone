export const PROFILE_COMPLETENESS_PUBLIC_FIELDS = [
  "username",
  "displayName",
  "state",
  "city",
  "motherTongue",
  "bio",
  "religion",
  "education",
  "profession",
  "heightCm",
  "maritalStatus",
  "familyType",
  "familyValues",
  "diet",
  "countryOfResidence",
  "employer",
  "aboutFamily",
  "spokenLanguages",
] as const;

export const PROFILE_COMPLETENESS_PRIVATE_FIELDS = [
  "dateOfBirth",
  "incomeBand",
] as const;

function nonBlank(value: unknown): boolean {
  if (typeof value === "string") return value.trim().length > 0;
  if (Array.isArray(value)) return value.length > 0;
  return false;
}

function positiveNumber(value: unknown): boolean {
  return typeof value === "number" && Number.isFinite(value) && value > 0;
}

/**
 * Mirrors the Android profile wizard's 20-field completeness contract while keeping the
 * authoritative score on the backend. Sensitive values remain in userPrivate and are used only
 * for this aggregate calculation; they are never copied into the public profile.
 */
export function calculateProfileCompletenessValue(
  publicProfile: Record<string, unknown>,
  privateProfile: Record<string, unknown>
): number {
  const checks = [
    nonBlank(publicProfile.username),
    nonBlank(publicProfile.displayName),
    nonBlank(privateProfile.dateOfBirth),
    nonBlank(publicProfile.state),
    nonBlank(publicProfile.city),
    nonBlank(publicProfile.motherTongue),
    nonBlank(publicProfile.bio),
    nonBlank(publicProfile.religion),
    nonBlank(publicProfile.education),
    nonBlank(publicProfile.profession),
    positiveNumber(publicProfile.heightCm),
    nonBlank(publicProfile.maritalStatus),
    nonBlank(publicProfile.familyType),
    nonBlank(publicProfile.familyValues),
    nonBlank(publicProfile.diet),
    nonBlank(publicProfile.countryOfResidence),
    nonBlank(privateProfile.incomeBand),
    nonBlank(publicProfile.employer),
    nonBlank(publicProfile.aboutFamily),
    nonBlank(publicProfile.spokenLanguages),
  ];

  const completed = checks.filter(Boolean).length;
  return Math.round((completed / checks.length) * 1000) / 1000;
}
