export const FAMILY_ROLES = ["PARENT", "SIBLING", "GUARDIAN"] as const;
export type FamilyRole = typeof FAMILY_ROLES[number];

export const FAMILY_PERMISSIONS = [
  "VIEW_PROFILE",
  "EDIT_PROFILE",
] as const;
export type FamilyPermission = typeof FAMILY_PERMISSIONS[number];

const EDITABLE_STRING_FIELDS = new Set([
  "city",
  "bio",
  "education",
  "profession",
  "maritalStatus",
  "diet",
  "familyType",
  "familyStatus",
  "familyValues",
  "fatherOccupation",
  "motherOccupation",
  "nativeState",
  "countryOfResidence",
  "citizenship",
]);

const EDITABLE_BOOLEAN_FIELDS = new Set([
  "isNRI",
  "willingToRelocate",
]);

const EDITABLE_NUMBER_FIELDS = new Set([
  "heightCm",
  "siblings",
]);

const EDITABLE_STRING_ARRAY_FIELDS = new Set([
  "hobbies",
  "spokenLanguages",
]);

export function normalizeFamilyRole(value: unknown): FamilyRole | null {
  const role = typeof value === "string" ? value.trim().toUpperCase() : "";
  return FAMILY_ROLES.includes(role as FamilyRole) ? role as FamilyRole : null;
}

export function normalizeFamilyPermissions(value: unknown): FamilyPermission[] {
  if (!Array.isArray(value)) return [];
  const result = value
    .filter((item): item is string => typeof item === "string")
    .map((item) => item.trim().toUpperCase())
    .filter((item): item is FamilyPermission =>
      FAMILY_PERMISSIONS.includes(item as FamilyPermission)
    );
  return [...new Set(result)];
}

export function sanitizeFamilyProfilePatch(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  const input = value as Record<string, unknown>;
  const output: Record<string, unknown> = {};

  for (const [key, raw] of Object.entries(input)) {
    if (EDITABLE_STRING_FIELDS.has(key)) {
      if (typeof raw !== "string") continue;
      const max = key === "bio" ? 1000 : 160;
      output[key] = raw.trim().slice(0, max);
      continue;
    }
    if (EDITABLE_BOOLEAN_FIELDS.has(key) && typeof raw === "boolean") {
      output[key] = raw;
      continue;
    }
    if (EDITABLE_NUMBER_FIELDS.has(key)) {
      const number = Number(raw);
      if (!Number.isFinite(number)) continue;
      if (key === "heightCm" && (number < 100 || number > 250)) continue;
      if (key === "siblings" && (number < 0 || number > 20)) continue;
      output[key] = Math.trunc(number);
      continue;
    }
    if (EDITABLE_STRING_ARRAY_FIELDS.has(key) && Array.isArray(raw)) {
      output[key] = raw
        .filter((item): item is string => typeof item === "string")
        .map((item) => item.trim().slice(0, 80))
        .filter(Boolean)
        .slice(0, 30);
    }
  }
  return output;
}

export function familyPatchContainsForbiddenFields(
  original: unknown,
  sanitized: Record<string, unknown>
): boolean {
  if (!original || typeof original !== "object" || Array.isArray(original)) return true;
  return Object.keys(original as Record<string, unknown>)
    .some((key) => !(key in sanitized));
}
