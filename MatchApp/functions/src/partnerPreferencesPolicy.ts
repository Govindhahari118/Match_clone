export type PreferenceMode = "STRICT" | "PREFERRED" | "NO_PREFERENCE";

export type PartnerPreferenceDocument = {
  configured: boolean;
  sharePublicSummary: boolean;
  ageMode: PreferenceMode;
  ageMin: number;
  ageMax: number;
  heightMode: PreferenceMode;
  heightMinCm: number;
  heightMaxCm: number;
  weightMode: PreferenceMode;
  weightMinKg: number;
  weightMaxKg: number;
  incomeBandMode: PreferenceMode;
  incomeBands: string[];
  complexionMode: PreferenceMode;
  complexions: string[];
  religionMode: PreferenceMode;
  religions: string[];
  casteMode: PreferenceMode;
  castes: string[];
  subCasteMode: PreferenceMode;
  subCastes: string[];
  gothraMode: PreferenceMode;
  gothras: string[];
  faithTraditionMode: PreferenceMode;
  faithTraditions: string[];
  faithSubTraditionMode: PreferenceMode;
  faithSubTraditions: string[];
  faithInstitutionMode: PreferenceMode;
  faithInstitutions: string[];
  stateMode: PreferenceMode;
  states: string[];
  nativeStateMode: PreferenceMode;
  nativeStates: string[];
  cityMode: PreferenceMode;
  cities: string[];
  motherTongueMode: PreferenceMode;
  motherTongues: string[];
  maritalStatusMode: PreferenceMode;
  maritalStatuses: string[];
  educationMode: PreferenceMode;
  educationLevels: string[];
  educationFieldMode: PreferenceMode;
  educationFields: string[];
  occupationMode: PreferenceMode;
  occupationCategories: string[];
  employerTypeMode: PreferenceMode;
  employerTypes: string[];
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
  familyStatusMode: PreferenceMode;
  familyStatuses: string[];
  familyValuesMode: PreferenceMode;
  familyValues: string[];
  physicalStatusMode: PreferenceMode;
  physicalStatuses: string[];
  residentialStatusMode: PreferenceMode;
  residentialStatuses: string[];
  visaStatusMode: PreferenceMode;
  visaStatuses: string[];
};

export const DEFAULT_PARTNER_PREFERENCES: PartnerPreferenceDocument = {
  configured: false,
  sharePublicSummary: false,
  ageMode: "NO_PREFERENCE",
  ageMin: 18,
  ageMax: 70,
  heightMode: "NO_PREFERENCE",
  heightMinCm: 90,
  heightMaxCm: 250,
  weightMode: "NO_PREFERENCE",
  weightMinKg: 30,
  weightMaxKg: 250,
  incomeBandMode: "NO_PREFERENCE",
  incomeBands: [],
  complexionMode: "NO_PREFERENCE",
  complexions: [],
  religionMode: "NO_PREFERENCE",
  religions: [],
  casteMode: "NO_PREFERENCE",
  castes: [],
  subCasteMode: "NO_PREFERENCE",
  subCastes: [],
  gothraMode: "NO_PREFERENCE",
  gothras: [],
  faithTraditionMode: "NO_PREFERENCE",
  faithTraditions: [],
  faithSubTraditionMode: "NO_PREFERENCE",
  faithSubTraditions: [],
  faithInstitutionMode: "NO_PREFERENCE",
  faithInstitutions: [],
  stateMode: "NO_PREFERENCE",
  states: [],
  nativeStateMode: "NO_PREFERENCE",
  nativeStates: [],
  cityMode: "NO_PREFERENCE",
  cities: [],
  motherTongueMode: "NO_PREFERENCE",
  motherTongues: [],
  maritalStatusMode: "NO_PREFERENCE",
  maritalStatuses: [],
  educationMode: "NO_PREFERENCE",
  educationLevels: [],
  educationFieldMode: "NO_PREFERENCE",
  educationFields: [],
  occupationMode: "NO_PREFERENCE",
  occupationCategories: [],
  employerTypeMode: "NO_PREFERENCE",
  employerTypes: [],
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
  familyStatusMode: "NO_PREFERENCE",
  familyStatuses: [],
  familyValuesMode: "NO_PREFERENCE",
  familyValues: [],
  physicalStatusMode: "NO_PREFERENCE",
  physicalStatuses: [],
  residentialStatusMode: "NO_PREFERENCE",
  residentialStatuses: [],
  visaStatusMode: "NO_PREFERENCE",
  visaStatuses: [],
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

function modeForValues(modeValue: unknown, valuesValue: unknown): PreferenceMode {
  return strings(valuesValue).length > 0 ? mode(modeValue) : "NO_PREFERENCE";
}

function choiceStrings(value: unknown, allowed: readonly string[]): string[] {
  if (!Array.isArray(value)) return [];
  const byKey = new Map(allowed.map((item) => [item.toUpperCase(), item]));
  const selected = new Set<string>();
  for (const item of value) {
    if (typeof item !== "string") continue;
    const canonical = byKey.get(item.trim().toUpperCase());
    if (canonical) selected.add(canonical);
  }
  return [...selected];
}

function modeForChoices(
  modeValue: unknown,
  valuesValue: unknown,
  allowed: readonly string[]
): PreferenceMode {
  return choiceStrings(valuesValue, allowed).length > 0
    ? mode(modeValue)
    : "NO_PREFERENCE";
}

const CHILDREN_CHOICES = ["HAS_CHILDREN", "NO_CHILDREN"] as const;
const NRI_CHOICES = ["NRI", "INDIA_RESIDENT"] as const;
const RELOCATION_CHOICES = [
  "WILLING_TO_RELOCATE",
  "NOT_WILLING_TO_RELOCATE",
] as const;

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
  const weightMinKg = integer(raw.weightMinKg, 30, 30, 250);
  const weightMaxKg = Math.max(
    weightMinKg,
    integer(raw.weightMaxKg, 250, 30, 250)
  );

  return {
    configured: raw.configured === true,
    sharePublicSummary: raw.sharePublicSummary === true,
    ageMode: mode(raw.ageMode),
    ageMin,
    ageMax,
    heightMode: mode(raw.heightMode),
    heightMinCm,
    heightMaxCm,
    weightMode: mode(raw.weightMode),
    weightMinKg,
    weightMaxKg,
    incomeBandMode: modeForValues(raw.incomeBandMode, raw.incomeBands),
    incomeBands: strings(raw.incomeBands),
    complexionMode: modeForValues(raw.complexionMode, raw.complexions),
    complexions: strings(raw.complexions),
    religionMode: modeForValues(raw.religionMode, raw.religions),
    religions: strings(raw.religions),
    casteMode: modeForValues(raw.casteMode, raw.castes),
    castes: strings(raw.castes),
    subCasteMode: modeForValues(raw.subCasteMode, raw.subCastes),
    subCastes: strings(raw.subCastes),
    gothraMode: modeForValues(raw.gothraMode, raw.gothras),
    gothras: strings(raw.gothras),
    faithTraditionMode: modeForValues(raw.faithTraditionMode, raw.faithTraditions),
    faithTraditions: strings(raw.faithTraditions),
    faithSubTraditionMode: modeForValues(raw.faithSubTraditionMode, raw.faithSubTraditions),
    faithSubTraditions: strings(raw.faithSubTraditions),
    faithInstitutionMode: modeForValues(raw.faithInstitutionMode, raw.faithInstitutions),
    faithInstitutions: strings(raw.faithInstitutions),
    stateMode: modeForValues(raw.stateMode, raw.states),
    states: strings(raw.states),
    nativeStateMode: modeForValues(raw.nativeStateMode, raw.nativeStates),
    nativeStates: strings(raw.nativeStates),
    cityMode: modeForValues(raw.cityMode, raw.cities),
    cities: strings(raw.cities),
    motherTongueMode: modeForValues(raw.motherTongueMode, raw.motherTongues),
    motherTongues: strings(raw.motherTongues),
    maritalStatusMode: modeForValues(raw.maritalStatusMode, raw.maritalStatuses),
    maritalStatuses: strings(raw.maritalStatuses),
    educationMode: modeForValues(raw.educationMode, raw.educationLevels),
    educationLevels: strings(raw.educationLevels),
    educationFieldMode: modeForValues(raw.educationFieldMode, raw.educationFields),
    educationFields: strings(raw.educationFields),
    occupationMode: modeForValues(raw.occupationMode, raw.occupationCategories),
    occupationCategories: strings(raw.occupationCategories),
    employerTypeMode: modeForValues(raw.employerTypeMode, raw.employerTypes),
    employerTypes: strings(raw.employerTypes),
    dietMode: modeForValues(raw.dietMode, raw.diets),
    diets: strings(raw.diets),
    smokingMode: modeForValues(raw.smokingMode, raw.smoking),
    smoking: strings(raw.smoking),
    drinkingMode: modeForValues(raw.drinkingMode, raw.drinking),
    drinking: strings(raw.drinking),
    countryOfResidenceMode: modeForValues(
      raw.countryOfResidenceMode,
      raw.countriesOfResidence
    ),
    countriesOfResidence: strings(raw.countriesOfResidence),
    citizenshipMode: modeForValues(raw.citizenshipMode, raw.citizenships),
    citizenships: strings(raw.citizenships),
    childrenMode: modeForChoices(
      raw.childrenMode,
      raw.childrenStatuses,
      CHILDREN_CHOICES
    ),
    childrenStatuses: choiceStrings(raw.childrenStatuses, CHILDREN_CHOICES),
    nriMode: modeForChoices(raw.nriMode, raw.nriStatuses, NRI_CHOICES),
    nriStatuses: choiceStrings(raw.nriStatuses, NRI_CHOICES),
    relocationMode: modeForChoices(
      raw.relocationMode,
      raw.relocationStatuses,
      RELOCATION_CHOICES
    ),
    relocationStatuses: choiceStrings(raw.relocationStatuses, RELOCATION_CHOICES),
    familyTypeMode: modeForValues(raw.familyTypeMode, raw.familyTypes),
    familyTypes: strings(raw.familyTypes),
    familyStatusMode: modeForValues(raw.familyStatusMode, raw.familyStatuses),
    familyStatuses: strings(raw.familyStatuses),
    familyValuesMode: modeForValues(raw.familyValuesMode, raw.familyValues),
    familyValues: strings(raw.familyValues),
    physicalStatusMode: modeForValues(
      raw.physicalStatusMode,
      raw.physicalStatuses
    ),
    physicalStatuses: strings(raw.physicalStatuses),
    residentialStatusMode: modeForValues(
      raw.residentialStatusMode,
      raw.residentialStatuses
    ),
    residentialStatuses: strings(raw.residentialStatuses),
    visaStatusMode: modeForValues(raw.visaStatusMode, raw.visaStatuses),
    visaStatuses: strings(raw.visaStatuses),
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
  if (subject.isNRI === true) return "NRI";
  if (!country) return "UNKNOWN";
  return country === "india" ? "INDIA_RESIDENT" : "NRI";
}

function childrenClass(subject: Record<string, unknown>): string {
  if (typeof subject.hasChildren !== "boolean") return "UNKNOWN";
  return subject.hasChildren ? "HAS_CHILDREN" : "NO_CHILDREN";
}

function relocationClass(subject: Record<string, unknown>): string {
  if (typeof subject.willingToRelocate !== "boolean") return "UNKNOWN";
  return subject.willingToRelocate
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

  const weight = Number(subject.weight || 0);
  if (
    preferences.weightMode === "STRICT" &&
    (!Number.isFinite(weight) ||
      weight < preferences.weightMinKg ||
      weight > preferences.weightMaxKg)
  ) return false;

  return strictListAllows(preferences.incomeBandMode, preferences.incomeBands, subject.incomeBand) &&
    strictListAllows(preferences.complexionMode, preferences.complexions, subject.complexion) &&
    strictListAllows(preferences.religionMode, preferences.religions, subject.religion) &&
    strictListAllows(preferences.casteMode, preferences.castes, subject.caste) &&
    strictListAllows(preferences.subCasteMode, preferences.subCastes, subject.subCaste) &&
    strictListAllows(preferences.gothraMode, preferences.gothras, subject.gothra) &&
    strictListAllows(
      preferences.faithTraditionMode,
      preferences.faithTraditions,
      subject.faithTradition
    ) &&
    strictListAllows(
      preferences.faithSubTraditionMode,
      preferences.faithSubTraditions,
      subject.faithSubTradition
    ) &&
    strictListAllows(
      preferences.faithInstitutionMode,
      preferences.faithInstitutions,
      subject.faithInstitution
    ) &&
    strictListAllows(preferences.stateMode, preferences.states, subject.state) &&
    strictListAllows(preferences.nativeStateMode, preferences.nativeStates, subject.nativeState) &&
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
      preferences.educationFieldMode,
      preferences.educationFields,
      subject.educationField
    ) &&
    strictListAllows(
      preferences.occupationMode,
      preferences.occupationCategories,
      subject.occupationCategory
    ) &&
    strictListAllows(preferences.employerTypeMode, preferences.employerTypes, subject.employerType) &&
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
    strictListAllows(preferences.familyStatusMode, preferences.familyStatuses, subject.familyStatus) &&
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
    ) &&
    strictListAllows(preferences.visaStatusMode, preferences.visaStatuses, subject.visaStatus);
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

  const weight = Number(subject.weight || 0);
  if (preferences.weightMode === "PREFERRED") {
    possible += 1;
    if (
      Number.isFinite(weight) &&
      weight >= preferences.weightMinKg &&
      weight <= preferences.weightMaxKg
    ) earned += 1;
  }

  const listChecks = [
    preferredListScore(preferences.incomeBandMode, preferences.incomeBands, subject.incomeBand),
    preferredListScore(preferences.complexionMode, preferences.complexions, subject.complexion),
    preferredListScore(preferences.religionMode, preferences.religions, subject.religion),
    preferredListScore(preferences.casteMode, preferences.castes, subject.caste),
    preferredListScore(preferences.subCasteMode, preferences.subCastes, subject.subCaste),
    preferredListScore(preferences.gothraMode, preferences.gothras, subject.gothra),
    preferredListScore(
      preferences.faithTraditionMode,
      preferences.faithTraditions,
      subject.faithTradition
    ),
    preferredListScore(
      preferences.faithSubTraditionMode,
      preferences.faithSubTraditions,
      subject.faithSubTradition
    ),
    preferredListScore(
      preferences.faithInstitutionMode,
      preferences.faithInstitutions,
      subject.faithInstitution
    ),
    preferredListScore(preferences.stateMode, preferences.states, subject.state),
    preferredListScore(preferences.nativeStateMode, preferences.nativeStates, subject.nativeState),
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
      preferences.educationFieldMode,
      preferences.educationFields,
      subject.educationField
    ),
    preferredListScore(
      preferences.occupationMode,
      preferences.occupationCategories,
      subject.occupationCategory
    ),
    preferredListScore(preferences.employerTypeMode, preferences.employerTypes, subject.employerType),
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
    preferredListScore(preferences.familyStatusMode, preferences.familyStatuses, subject.familyStatus),
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
    preferredListScore(preferences.visaStatusMode, preferences.visaStatuses, subject.visaStatus),
  ];
  for (const result of listChecks) {
    earned += result.earned;
    possible += result.possible;
  }

  return possible > 0 ? earned / possible : null;
}

export const STRONG_MUTUAL_MIN_CRITERIA = 5;

function activePreferenceFit(
  preferences: PartnerPreferenceDocument,
  subject: Record<string, unknown>
): { score: number | null; criteria: number } {
  let earned = 0;
  let possible = 0;
  const scoreRange = (
    prefMode: PreferenceMode,
    value: number,
    min: number,
    max: number
  ) => {
    if (prefMode === "NO_PREFERENCE") return;
    possible += 1;
    if (Number.isFinite(value) && value >= min && value <= max) earned += 1;
  };
  const scoreList = (
    prefMode: PreferenceMode,
    expected: string[],
    actual: unknown
  ) => {
    if (prefMode === "NO_PREFERENCE" || expected.length === 0) return;
    possible += 1;
    if (sameOne(expected, actual)) earned += 1;
  };

  scoreRange(preferences.ageMode, Number(subject.age || 0), preferences.ageMin, preferences.ageMax);
  scoreRange(
    preferences.heightMode,
    Number(subject.heightCm || 0),
    preferences.heightMinCm,
    preferences.heightMaxCm
  );
  scoreRange(
    preferences.weightMode,
    Number(subject.weight || 0),
    preferences.weightMinKg,
    preferences.weightMaxKg
  );

  const listRules: Array<[PreferenceMode, string[], unknown]> = [
    [preferences.incomeBandMode, preferences.incomeBands, subject.incomeBand],
    [preferences.complexionMode, preferences.complexions, subject.complexion],
    [preferences.religionMode, preferences.religions, subject.religion],
    [preferences.casteMode, preferences.castes, subject.caste],
    [preferences.subCasteMode, preferences.subCastes, subject.subCaste],
    [preferences.gothraMode, preferences.gothras, subject.gothra],
    [preferences.faithTraditionMode, preferences.faithTraditions, subject.faithTradition],
    [preferences.faithSubTraditionMode, preferences.faithSubTraditions, subject.faithSubTradition],
    [preferences.faithInstitutionMode, preferences.faithInstitutions, subject.faithInstitution],
    [preferences.stateMode, preferences.states, subject.state],
    [preferences.nativeStateMode, preferences.nativeStates, subject.nativeState],
    [preferences.cityMode, preferences.cities, subject.city],
    [preferences.motherTongueMode, preferences.motherTongues, subject.motherTongue],
    [preferences.maritalStatusMode, preferences.maritalStatuses, subject.maritalStatus],
    [preferences.educationMode, preferences.educationLevels, subject.education],
    [preferences.educationFieldMode, preferences.educationFields, subject.educationField],
    [preferences.occupationMode, preferences.occupationCategories, subject.occupationCategory],
    [preferences.employerTypeMode, preferences.employerTypes, subject.employerType],
    [preferences.dietMode, preferences.diets, subject.diet],
    [preferences.smokingMode, preferences.smoking, subject.smoking],
    [preferences.drinkingMode, preferences.drinking, subject.drinking],
    [preferences.countryOfResidenceMode, preferences.countriesOfResidence, subject.countryOfResidence],
    [preferences.citizenshipMode, preferences.citizenships, subject.citizenship],
    [preferences.childrenMode, preferences.childrenStatuses, childrenClass(subject)],
    [preferences.nriMode, preferences.nriStatuses, residenceClass(subject)],
    [preferences.relocationMode, preferences.relocationStatuses, relocationClass(subject)],
    [preferences.familyTypeMode, preferences.familyTypes, subject.familyType],
    [preferences.familyStatusMode, preferences.familyStatuses, subject.familyStatus],
    [preferences.familyValuesMode, preferences.familyValues, subject.familyValues],
    [preferences.physicalStatusMode, preferences.physicalStatuses, subject.physicalStatus],
    [preferences.residentialStatusMode, preferences.residentialStatuses, subject.residentialStatus],
    [preferences.visaStatusMode, preferences.visaStatuses, subject.visaStatus],
  ];
  listRules.forEach(([prefMode, expected, actual]) => scoreList(prefMode, expected, actual));
  return {
    score: possible > 0 ? earned / possible : null,
    criteria: possible,
  };
}

export type BilateralPreferenceMatch = {
  forward: number | null;
  reverse: number | null;
  mutual: number | null;
  forwardCriteria: number;
  reverseCriteria: number;
  mutualCriteria: number;
  formulaVersion: "partner-preferences-v4-reciprocal-min-evidence";
};

export function bilateralPreferenceMatch(
  viewerPreferences: PartnerPreferenceDocument,
  viewer: Record<string, unknown>,
  candidatePreferences: PartnerPreferenceDocument,
  candidate: Record<string, unknown>
): BilateralPreferenceMatch {
  const forwardFit = activePreferenceFit(viewerPreferences, candidate);
  const reverseFit = activePreferenceFit(candidatePreferences, viewer);
  const forward = forwardFit.score;
  const reverse = reverseFit.score;
  const mutual = forward != null && reverse != null
    ? Math.min(forward, reverse)
    : null;
  return {
    forward,
    reverse,
    mutual,
    forwardCriteria: forwardFit.criteria,
    reverseCriteria: reverseFit.criteria,
    mutualCriteria: Math.min(forwardFit.criteria, reverseFit.criteria),
    formulaVersion: "partner-preferences-v4-reciprocal-min-evidence",
  };
}

/** Backward-compatible scalar used by older ranking callers. */
export function bilateralPreferredFit(
  viewerPreferences: PartnerPreferenceDocument,
  viewer: Record<string, unknown>,
  candidatePreferences: PartnerPreferenceDocument,
  candidate: Record<string, unknown>
): number | null {
  return bilateralPreferenceMatch(
    viewerPreferences,
    viewer,
    candidatePreferences,
    candidate
  ).mutual;
}
