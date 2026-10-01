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
  "occupationCategory",
  "heightCm",
  "weight",
  "maritalStatus",
  "familyType",
  "familyValues",
  "fatherOccupation",
  "motherOccupation",
  "diet",
  "smoking",
  "drinking",
  "hobbies",
  "countryOfResidence",
  "employer",
  "aboutFamily",
  "spokenLanguages",
  "photoUrl",
  "isVerified",
  "verificationLevel",
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

function section(weight: number, checks: boolean[]): number {
  if (checks.length === 0) return 0;
  return weight * checks.filter(Boolean).length / checks.length;
}

/**
 * Server-authoritative weighted profile strength.
 *
 * The weighting mirrors the frozen production plan rather than treating every field as equally
 * valuable. A deliberate NO_PREFERENCE selection still completes the preference section because
 * "no preference" is valid partner intent, while verification remains evidence-based.
 */
export function calculateProfileCompletenessValue(
  publicProfile: Record<string, unknown>,
  privateProfile: Record<string, unknown>,
  partnerPreferences: Record<string, unknown> = {},
  verification: Record<string, unknown> = {}
): number {
  let score = 0;

  // Basics — 15%
  score += section(0.15, [
    nonBlank(publicProfile.username),
    nonBlank(publicProfile.displayName),
    nonBlank(privateProfile.dateOfBirth),
    nonBlank(publicProfile.state),
    nonBlank(publicProfile.city),
    nonBlank(publicProfile.motherTongue),
    nonBlank(publicProfile.religion),
    positiveNumber(publicProfile.heightCm),
    positiveNumber(publicProfile.weight),
    nonBlank(publicProfile.maritalStatus),
  ]);

  // Approved primary photo — 15%
  score += section(0.15, [nonBlank(publicProfile.photoUrl)]);

  // Education / career — 10%
  score += section(0.10, [
    nonBlank(publicProfile.education),
    nonBlank(publicProfile.profession),
    nonBlank(publicProfile.occupationCategory),
    nonBlank(publicProfile.employer),
    nonBlank(privateProfile.incomeBand),
  ]);

  // Family — 10%
  score += section(0.10, [
    nonBlank(publicProfile.familyType),
    nonBlank(publicProfile.familyValues),
    nonBlank(publicProfile.aboutFamily),
    nonBlank(publicProfile.fatherOccupation) || nonBlank(publicProfile.motherOccupation),
  ]);

  // Lifestyle — 10%
  score += section(0.10, [
    nonBlank(publicProfile.diet),
    nonBlank(publicProfile.smoking),
    nonBlank(publicProfile.drinking),
    nonBlank(publicProfile.hobbies),
    nonBlank(publicProfile.spokenLanguages),
  ]);

  // Partner preferences — 15%. Explicit NO_PREFERENCE is a completed choice.
  score += section(0.15, [partnerPreferences.configured === true]);

  // About me — 5%
  score += section(0.05, [nonBlank(publicProfile.bio)]);

  // Verification — 20%: phone evidence contributes 40% of the section, reviewed identity 60%.
  const phoneVerified = String(verification.phoneStatus || "").toUpperCase() === "VERIFIED";
  const identityVerified =
    publicProfile.isVerified === true ||
    Number(publicProfile.verificationLevel || 0) >= 2;
  score += 0.20 * ((phoneVerified ? 0.4 : 0) + (identityVerified ? 0.6 : 0));

  return Math.round(Math.max(0, Math.min(1, score)) * 1000) / 1000;
}
