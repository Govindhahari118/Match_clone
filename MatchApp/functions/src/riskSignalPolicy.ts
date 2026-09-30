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
