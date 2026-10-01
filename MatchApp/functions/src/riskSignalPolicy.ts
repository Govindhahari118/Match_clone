export const PROFILE_MUTATION_REVIEW_THRESHOLD_PER_DAY = 20;

const PROFILE_MUTATION_FIELDS = [
  "displayName",
  "age",
  "gender",
  "lookingFor",
  "city",
  "state",
  "bio",
  "religion",
  "caste",
  "subCaste",
  "motherTongue",
  "education",
  "educationField",
  "profession",
  "occupationCategory",
  "employer",
  "employerType",
  "maritalStatus",
  "heightCm",
  "diet",
  "smoking",
  "drinking",
  "familyType",
  "familyStatus",
  "physicalStatus",
  "hobbies",
  "spokenLanguages",
  "nativeState",
  "countryOfResidence",
  "citizenship",
  "isNRI",
  "willingToRelocate",
  "familyValues",
  "aboutFamily",
] as const;

export function deviceAccountSwitchIsReviewSignal(
  priorUid: unknown,
  currentUid: string
): boolean {
  return typeof priorUid === "string" &&
    priorUid.length > 0 &&
    priorUid !== currentUid;
}

export function profileMutationFieldsChanged(
  before: Record<string, unknown>,
  after: Record<string, unknown>
): boolean {
  return PROFILE_MUTATION_FIELDS.some((field) =>
    JSON.stringify(before[field] ?? null) !== JSON.stringify(after[field] ?? null)
  );
}

export function playAccountOwnershipMismatch(
  expectedAccountHash: string,
  providerAccountHash: unknown
): boolean {
  return typeof providerAccountHash !== "string" ||
    providerAccountHash !== expectedAccountHash;
}


const MIN_DUPLICATE_BIO_CHARS = 80;
const MIN_DUPLICATE_BIO_WORDS = 15;

/**
 * Normalizes only enough to catch substantially identical copied bios. Short/generic text is
 * excluded to reduce false positives; the normalized text is never stored, only its backend hash.
 */
export function normalizeBioForDuplicateReview(value: unknown): string | undefined {
  if (typeof value !== "string") return undefined;
  const normalized = value
    .normalize("NFKC")
    .toLocaleLowerCase("en-IN")
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .replace(/\s+/g, " ")
    .trim();
  if (normalized.length < MIN_DUPLICATE_BIO_CHARS) return undefined;
  if (normalized.split(" ").filter(Boolean).length < MIN_DUPLICATE_BIO_WORDS) return undefined;
  return normalized;
}
