export type PreferenceMode = "STRICT" | "PREFERRED" | "NO_PREFERENCE";

export type PartnerPreferenceDocument = {
  configured: boolean;
  ageMode: PreferenceMode;
  ageMin: number;
  ageMax: number;
  heightMode: PreferenceMode;
  heightMinCm: number;
  heightMaxCm: number;
  religionMode: PreferenceMode;
  religions: string[];
  casteMode: PreferenceMode;
  castes: string[];
  subCasteMode: PreferenceMode;
  subCastes: string[];
  stateMode: PreferenceMode;
  states: string[];
  cityMode: PreferenceMode;
  cities: string[];
  motherTongueMode: PreferenceMode;
  motherTongues: string[];
  maritalStatusMode: PreferenceMode;
  maritalStatuses: string[];
  educationMode: PreferenceMode;
  educationLevels: string[];
  occupationMode: PreferenceMode;
  occupationCategories: string[];
  dietMode: PreferenceMode;
  diets: string[];
  smokingMode: PreferenceMode;
  smoking: string[];
  drinkingMode: PreferenceMode;
  drinking: string[];
  countryOfResidenceMode: PreferenceMode;
  countriesOfResidence: string[];
  citizenshipMode: PreferenceMode;
  citizenships: string[];
  childrenMode: PreferenceMode;
  childrenStatuses: string[];
  nriMode: PreferenceMode;
  nriStatuses: string[];
  relocationMode: PreferenceMode;
  relocationStatuses: string[];
  familyTypeMode: PreferenceMode;
  familyTypes: string[];
  familyValuesMode: PreferenceMode;
  familyValues: string[];
  physicalStatusMode: PreferenceMode;
  physicalStatuses: string[];
  residentialStatusMode: PreferenceMode;
  residentialStatuses: string[];
};

export const DEFAULT_PARTNER_PREFERENCES: PartnerPreferenceDocument = {
  configured: false,
  ageMode: "NO_PREFERENCE",
  ageMin: 18,
  ageMax: 70,
  heightMode: "NO_PREFERENCE",
  heightMinCm: 90,
  heightMaxCm: 250,
  religionMode: "NO_PREFERENCE",
  religions: [],
  casteMode: "NO_PREFERENCE",
  castes: [],
  subCasteMode: "NO_PREFERENCE",
  subCastes: [],
  stateMode: "NO_PREFERENCE",
  states: [],
  cityMode: "NO_PREFERENCE",
  cities: [],
  motherTongueMode: "NO_PREFERENCE",
  motherTongues: [],
  maritalStatusMode: "NO_PREFERENCE",
  maritalStatuses: [],
  educationMode: "NO_PREFERENCE",
  educationLevels: [],
  occupationMode: "NO_PREFERENCE",
  occupationCategories: [],
  dietMode: "NO_PREFERENCE",
  diets: [],
  smokingMode: "NO_PREFERENCE",
  smoking: [],
  drinkingMode: "NO_PREFERENCE",
  drinking: [],
  countryOfResidenceMode: "NO_PREFERENCE",
  countriesOfResidence: [],
  citizenshipMode: "NO_PREFERENCE",
  citizenships: [],
  childrenMode: "NO_PREFERENCE",
  childrenStatuses: [],
  nriMode: "NO_PREFERENCE",
  nriStatuses: [],
  relocationMode: "NO_PREFERENCE",
  relocationStatuses: [],
  familyTypeMode: "NO_PREFERENCE",
  familyTypes: [],
  familyValuesMode: "NO_PREFERENCE",
  familyValues: [],
  physicalStatusMode: "NO_PREFERENCE",
  physicalStatuses: [],
  residentialStatusMode: "NO_PREFERENCE",
  residentialStatuses: [],
};

const MODES = new Set<PreferenceMode>(["STRICT", "PREFERRED", "NO_PREFERENCE"]);

function mode(value: unknown): PreferenceMode {
  const normalized = typeof value === "string" ? value.trim().toUpperCase() : "";
  return MODES.has(normalized as PreferenceMode)
    ? normalized as PreferenceMode
    : "NO_PREFERENCE";
}

function integer(value: unknown, fallback: number, min: number, max: number): number {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.max(min, Math.min(max, Math.trunc(parsed)));
}

function strings(value: unknown, maxItems = 20): string[] {
  if (!Array.isArray(value)) return [];
  const unique = new Map<string, string>();
  for (const item of value) {
    if (typeof item !== "string") continue;
    const clean = item.trim().replace(/\s+/g, " ").slice(0, 80);
    if (!clean) continue;
    const key = clean.toLocaleLowerCase("en-IN");
    if (!unique.has(key)) unique.set(key, clean);
    if (unique.size >= maxItems) break;
  }
  return [...unique.values()];
}

export function normalizePartnerPreferences(
  value: unknown
): PartnerPreferenceDocument {
  const raw = value && typeof value === "object"
    ? value as Record<string, unknown>
    : {};

  const ageMin = integer(raw.ageMin, 18, 18, 99);
  const ageMax = Math.max(ageMin, integer(raw.ageMax, 70, 18, 99));
  const heightMinCm = integer(raw.heightMinCm, 90, 90, 250);
  const heightMaxCm = Math.max(
    heightMinCm,
    integer(raw.heightMaxCm, 250, 90, 250)
  );

  return {
    configured: raw.configured === true,
    ageMode: mode(raw.ageMode),
    ageMin,
    ageMax,
    heightMode: mode(raw.heightMode),
    heightMinCm,
    heightMaxCm,
    religionMode: mode(raw.religionMode),
    religions: strings(raw.religions),
    casteMode: mode(raw.casteMode),
    castes: strings(raw.castes),
    subCasteMode: mode(raw.subCasteMode),
    subCastes: strings(raw.subCastes),
    stateMode: mode(raw.stateMode),
    states: strings(raw.states),
    cityMode: mode(raw.cityMode),
    cities: strings(raw.cities),
    motherTongueMode: mode(raw.motherTongueMode),
    motherTongues: strings(raw.motherTongues),
    maritalStatusMode: mode(raw.maritalStatusMode),
    maritalStatuses: strings(raw.maritalStatuses),
    educationMode: mode(raw.educationMode),
    educationLevels: strings(raw.educationLevels),
    occupationMode: mode(raw.occupationMode),
    occupationCategories: strings(raw.occupationCategories),
    dietMode: mode(raw.dietMode),
    diets: strings(raw.diets),
    smokingMode: mode(raw.smokingMode),
    smoking: strings(raw.smoking),
    drinkingMode: mode(raw.drinkingMode),
    drinking: strings(raw.drinking),
    countryOfResidenceMode: mode(raw.countryOfResidenceMode),
    countriesOfResidence: strings(raw.countriesOfResidence),
    citizenshipMode: mode(raw.citizenshipMode),
    citizenships: strings(raw.citizenships),
    childrenMode: mode(raw.childrenMode),
    childrenStatuses: strings(raw.childrenStatuses),
    nriMode: mode(raw.nriMode),
    nriStatuses: strings(raw.nriStatuses),
    relocationMode: mode(raw.relocationMode),
    relocationStatuses: strings(raw.relocationStatuses),
    familyTypeMode: mode(raw.familyTypeMode),
    familyTypes: strings(raw.familyTypes),
    familyValuesMode: mode(raw.familyValuesMode),
    familyValues: strings(raw.familyValues),
    physicalStatusMode: mode(raw.physicalStatusMode),
    physicalStatuses: strings(raw.physicalStatuses),
    residentialStatusMode: mode(raw.residentialStatusMode),
    residentialStatuses: strings(raw.residentialStatuses),
  };
}

function sameOne(expected: string[], actual: unknown): boolean {
  if (expected.length === 0) return true;
  if (typeof actual !== "string") return false;
  const normalized = actual.trim().toLocaleLowerCase("en-IN");
  return expected.some((value) =>
    value.trim().toLocaleLowerCase("en-IN") === normalized
  );
}

function strictListAllows(
  prefMode: PreferenceMode,
  expected: string[],
  actual: unknown
): boolean {
  return prefMode !== "STRICT" || sameOne(expected, actual);
}

function preferredListScore(
  prefMode: PreferenceMode,
  expected: string[],
  actual: unknown
): { earned: number; possible: number } {
  if (prefMode !== "PREFERRED" || expected.length === 0) {
    return { earned: 0, possible: 0 };
  }
  return { earned: sameOne(expected, actual) ? 1 : 0, possible: 1 };
}


function residenceClass(subject: Record<string, unknown>): string {
  const country = typeof subject.countryOfResidence === "string"
    ? subject.countryOfResidence.trim().toLocaleLowerCase("en-IN")
    : "";
  return subject.isNRI === true || (country.length > 0 && country !== "india")
    ? "NRI"
    : "INDIA_RESIDENT";
}

function childrenClass(subject: Record<string, unknown>): string {
  return subject.hasChildren === true ? "HAS_CHILDREN" : "NO_CHILDREN";
}

function relocationClass(subject: Record<string, unknown>): string {
  return subject.willingToRelocate === true
    ? "WILLING_TO_RELOCATE"
    : "NOT_WILLING_TO_RELOCATE";
}

export function strictPreferencesAllow(
  preferences: PartnerPreferenceDocument,
  subject: Record<string, unknown>
): boolean {
  const age = Number(subject.age || 0);
  if (
    preferences.ageMode === "STRICT" &&
    (!Number.isFinite(age) || age < preferences.ageMin || age > preferences.ageMax)
  ) return false;

  const height = Number(subject.heightCm || 0);
  if (
    preferences.heightMode === "STRICT" &&
    (!Number.isFinite(height) ||
      height < preferences.heightMinCm ||
      height > preferences.heightMaxCm)
  ) return false;

  return strictListAllows(preferences.religionMode, preferences.religions, subject.religion) &&
    strictListAllows(preferences.casteMode, preferences.castes, subject.caste) &&
    strictListAllows(preferences.subCasteMode, preferences.subCastes, subject.subCaste) &&
    strictListAllows(preferences.stateMode, preferences.states, subject.state) &&
    strictListAllows(preferences.cityMode, preferences.cities, subject.city) &&
    strictListAllows(
      preferences.motherTongueMode,
      preferences.motherTongues,
      subject.motherTongue
    ) &&
    strictListAllows(
      preferences.maritalStatusMode,
      preferences.maritalStatuses,
      subject.maritalStatus
    ) &&
    strictListAllows(
      preferences.educationMode,
      preferences.educationLevels,
      subject.education
    ) &&
    strictListAllows(
      preferences.occupationMode,
      preferences.occupationCategories,
      subject.occupationCategory
    ) &&
    strictListAllows(preferences.dietMode, preferences.diets, subject.diet) &&
    strictListAllows(preferences.smokingMode, preferences.smoking, subject.smoking) &&
    strictListAllows(preferences.drinkingMode, preferences.drinking, subject.drinking) &&
    strictListAllows(
      preferences.countryOfResidenceMode,
      preferences.countriesOfResidence,
      subject.countryOfResidence
    ) &&
    strictListAllows(preferences.citizenshipMode, preferences.citizenships, subject.citizenship) &&
    strictListAllows(preferences.childrenMode, preferences.childrenStatuses, childrenClass(subject)) &&
    strictListAllows(preferences.nriMode, preferences.nriStatuses, residenceClass(subject)) &&
    strictListAllows(
      preferences.relocationMode,
      preferences.relocationStatuses,
      relocationClass(subject)
    ) &&
    strictListAllows(preferences.familyTypeMode, preferences.familyTypes, subject.familyType) &&
    strictListAllows(preferences.familyValuesMode, preferences.familyValues, subject.familyValues) &&
    strictListAllows(
      preferences.physicalStatusMode,
      preferences.physicalStatuses,
      subject.physicalStatus
    ) &&
    strictListAllows(
      preferences.residentialStatusMode,
      preferences.residentialStatuses,
      subject.residentialStatus
    );
}

/**
 * Preference affinity is intentionally separate from hard eligibility. Only PREFERRED criteria
 * contribute; NO_PREFERENCE and STRICT criteria never receive artificial neutral points.
 */
export function preferredPreferenceFit(
  preferences: PartnerPreferenceDocument,
  subject: Record<string, unknown>
): number | null {
  let earned = 0;
  let possible = 0;

  const age = Number(subject.age || 0);
  if (preferences.ageMode === "PREFERRED") {
    possible += 1;
    if (
      Number.isFinite(age) &&
      age >= preferences.ageMin &&
      age <= preferences.ageMax
    ) earned += 1;
  }

  const height = Number(subject.heightCm || 0);
  if (preferences.heightMode === "PREFERRED") {
    possible += 1;
    if (
      Number.isFinite(height) &&
      height >= preferences.heightMinCm &&
      height <= preferences.heightMaxCm
    ) earned += 1;
  }

  const listChecks = [
    preferredListScore(preferences.religionMode, preferences.religions, subject.religion),
    preferredListScore(preferences.casteMode, preferences.castes, subject.caste),
    preferredListScore(preferences.subCasteMode, preferences.subCastes, subject.subCaste),
    preferredListScore(preferences.stateMode, preferences.states, subject.state),
    preferredListScore(preferences.cityMode, preferences.cities, subject.city),
    preferredListScore(
      preferences.motherTongueMode,
      preferences.motherTongues,
      subject.motherTongue
    ),
    preferredListScore(
      preferences.maritalStatusMode,
      preferences.maritalStatuses,
      subject.maritalStatus
    ),
    preferredListScore(
      preferences.educationMode,
      preferences.educationLevels,
      subject.education
    ),
    preferredListScore(
      preferences.occupationMode,
      preferences.occupationCategories,
      subject.occupationCategory
    ),
    preferredListScore(preferences.dietMode, preferences.diets, subject.diet),
    preferredListScore(preferences.smokingMode, preferences.smoking, subject.smoking),
    preferredListScore(preferences.drinkingMode, preferences.drinking, subject.drinking),
    preferredListScore(
      preferences.countryOfResidenceMode,
      preferences.countriesOfResidence,
      subject.countryOfResidence
    ),
    preferredListScore(preferences.citizenshipMode, preferences.citizenships, subject.citizenship),
    preferredListScore(preferences.childrenMode, preferences.childrenStatuses, childrenClass(subject)),
    preferredListScore(preferences.nriMode, preferences.nriStatuses, residenceClass(subject)),
    preferredListScore(
      preferences.relocationMode,
      preferences.relocationStatuses,
      relocationClass(subject)
    ),
    preferredListScore(preferences.familyTypeMode, preferences.familyTypes, subject.familyType),
    preferredListScore(preferences.familyValuesMode, preferences.familyValues, subject.familyValues),
    preferredListScore(
      preferences.physicalStatusMode,
      preferences.physicalStatuses,
      subject.physicalStatus
    ),
    preferredListScore(
      preferences.residentialStatusMode,
      preferences.residentialStatuses,
      subject.residentialStatus
    ),
  ];
  for (const result of listChecks) {
    earned += result.earned;
    possible += result.possible;
  }

  return possible > 0 ? earned / possible : null;
}

export function bilateralPreferredFit(
  viewerPreferences: PartnerPreferenceDocument,
  viewer: Record<string, unknown>,
  candidatePreferences: PartnerPreferenceDocument,
  candidate: Record<string, unknown>
): number | null {
  const forward = preferredPreferenceFit(viewerPreferences, candidate);
  const reverse = preferredPreferenceFit(candidatePreferences, viewer);
  if (forward == null && reverse == null) return null;
  if (forward == null) return reverse;
  if (reverse == null) return forward;
  return (forward + reverse) / 2;
}
