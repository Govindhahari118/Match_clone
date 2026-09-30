const test = require("node:test");
const assert = require("node:assert/strict");

const {
  normalizePartnerPreferences,
  strictPreferencesAllow,
  preferredPreferenceFit,
  bilateralPreferredFit,
} = require("../lib/partnerPreferencesPolicy");

test("normalizes invalid ranges and unknown modes safely", () => {
  const prefs = normalizePartnerPreferences({
    configured: true,
    ageMode: "strict",
    ageMin: 30,
    ageMax: 22,
    cityMode: "preferred",
    cities: [" Hyderabad ", "hyderabad", "", 42],
    religionMode: "unexpected",
  });
  assert.equal(prefs.ageMode, "STRICT");
  assert.equal(prefs.ageMin, 30);
  assert.equal(prefs.ageMax, 30);
  assert.equal(prefs.cityMode, "PREFERRED");
  assert.deepEqual(prefs.cities, ["Hyderabad"]);
  assert.equal(prefs.religionMode, "NO_PREFERENCE");
});

test("STRICT preference excludes outside subject but PREFERRED does not", () => {
  const strict = normalizePartnerPreferences({
    ageMode: "STRICT",
    ageMin: 25,
    ageMax: 30,
    cityMode: "STRICT",
    cities: ["Hyderabad"],
  });
  assert.equal(strictPreferencesAllow(strict, { age: 27, city: "Hyderabad" }), true);
  assert.equal(strictPreferencesAllow(strict, { age: 31, city: "Hyderabad" }), false);
  assert.equal(strictPreferencesAllow(strict, { age: 27, city: "Pune" }), false);

  const preferred = normalizePartnerPreferences({
    cityMode: "PREFERRED",
    cities: ["Hyderabad"],
  });
  assert.equal(strictPreferencesAllow(preferred, { age: 27, city: "Pune" }), true);
});

test("preferred fit scores only explicitly preferred dimensions", () => {
  const prefs = normalizePartnerPreferences({
    cityMode: "PREFERRED",
    cities: ["Hyderabad"],
    religionMode: "PREFERRED",
    religions: ["Hindu"],
    dietMode: "NO_PREFERENCE",
  });
  assert.equal(preferredPreferenceFit(prefs, {
    city: "Hyderabad",
    religion: "Christian",
    diet: "Vegetarian",
  }), 0.5);
});

test("bilateral preferred fit averages both members without fabricating missing sides", () => {
  const a = normalizePartnerPreferences({
    cityMode: "PREFERRED",
    cities: ["Hyderabad"],
  });
  const b = normalizePartnerPreferences({
    educationMode: "PREFERRED",
    educationLevels: ["Masters"],
  });
  assert.equal(
    bilateralPreferredFit(
      a,
      { city: "Hyderabad", education: "Bachelors" },
      b,
      { city: "Hyderabad", education: "Masters" }
    ),
    0.5
  );

  const none = normalizePartnerPreferences({});
  assert.equal(
    bilateralPreferredFit(none, { city: "A" }, none, { city: "B" }),
    null
  );
});


test("expanded durable preferences cover community, NRI, children, relocation and family criteria", () => {
  const prefs = normalizePartnerPreferences({
    casteMode: "STRICT",
    castes: ["Reddy"],
    subCasteMode: "PREFERRED",
    subCastes: ["Pakanati"],
    countryOfResidenceMode: "PREFERRED",
    countriesOfResidence: ["United States"],
    citizenshipMode: "PREFERRED",
    citizenships: ["India"],
    childrenMode: "STRICT",
    childrenStatuses: ["NO_CHILDREN"],
    nriMode: "STRICT",
    nriStatuses: ["NRI"],
    relocationMode: "PREFERRED",
    relocationStatuses: ["WILLING_TO_RELOCATE"],
    familyTypeMode: "PREFERRED",
    familyTypes: ["Nuclear"],
    familyValuesMode: "PREFERRED",
    familyValues: ["Moderate"],
    physicalStatusMode: "STRICT",
    physicalStatuses: ["Normal"],
    residentialStatusMode: "PREFERRED",
    residentialStatuses: ["Work Visa"],
  });

  const matching = {
    caste: "Reddy",
    subCaste: "Pakanati",
    countryOfResidence: "United States",
    citizenship: "India",
    hasChildren: false,
    isNRI: true,
    willingToRelocate: true,
    familyType: "Nuclear",
    familyValues: "Moderate",
    physicalStatus: "Normal",
    residentialStatus: "Work Visa",
  };
  assert.equal(strictPreferencesAllow(prefs, matching), true);
  assert.equal(preferredPreferenceFit(prefs, matching), 1);
  assert.equal(strictPreferencesAllow(prefs, { ...matching, hasChildren: true }), false);
  assert.equal(strictPreferencesAllow(prefs, {
    ...matching,
    countryOfResidence: "India",
    isNRI: false,
  }), false);
  assert.equal(strictPreferencesAllow(prefs, { ...matching, caste: "Other" }), false);
});

test("residence classification treats overseas country as NRI even before isNRI backfill", () => {
  const prefs = normalizePartnerPreferences({
    nriMode: "STRICT",
    nriStatuses: ["NRI"],
  });
  assert.equal(
    strictPreferencesAllow(prefs, { countryOfResidence: "UAE", isNRI: false }),
    true
  );
  assert.equal(
    strictPreferencesAllow(prefs, { countryOfResidence: "India", isNRI: false }),
    false
  );
});

test("empty list modes normalize to no preference and finite choice values are whitelisted", () => {
  const prefs = normalizePartnerPreferences({
    casteMode: "STRICT",
    castes: [],
    childrenMode: "STRICT",
    childrenStatuses: ["UNKNOWN", "no_children"],
    nriMode: "PREFERRED",
    nriStatuses: ["nri", "bogus"],
    relocationMode: "STRICT",
    relocationStatuses: [],
  });
  assert.equal(prefs.casteMode, "NO_PREFERENCE");
  assert.equal(prefs.childrenMode, "STRICT");
  assert.deepEqual(prefs.childrenStatuses, ["NO_CHILDREN"]);
  assert.equal(prefs.nriMode, "PREFERRED");
  assert.deepEqual(prefs.nriStatuses, ["NRI"]);
  assert.equal(prefs.relocationMode, "NO_PREFERENCE");
});


test("strict finite-choice preferences reject profiles with missing data", () => {
  const prefs = normalizePartnerPreferences({
    childrenMode: "STRICT",
    childrenStatuses: ["NO_CHILDREN"],
    nriMode: "STRICT",
    nriStatuses: ["INDIA_RESIDENT"],
    relocationMode: "STRICT",
    relocationStatuses: ["NOT_WILLING_TO_RELOCATE"],
  });
  assert.equal(
    strictPreferencesAllow(prefs, {
      countryOfResidence: "",
    }),
    false
  );
});


test("remaining stable matrimony criteria participate in strict and preferred matching", () => {
  const prefs = normalizePartnerPreferences({
    gothraMode: "STRICT",
    gothras: ["Bharadwaja"],
    faithTraditionMode: "PREFERRED",
    faithTraditions: ["Vaishnava"],
    faithSubTraditionMode: "PREFERRED",
    faithSubTraditions: ["Sri Vaishnava"],
    faithInstitutionMode: "PREFERRED",
    faithInstitutions: ["Local Samaj"],
    nativeStateMode: "STRICT",
    nativeStates: ["Telangana"],
    educationFieldMode: "PREFERRED",
    educationFields: ["Engineering"],
    employerTypeMode: "PREFERRED",
    employerTypes: ["Private"],
    familyStatusMode: "PREFERRED",
    familyStatuses: ["Upper Middle Class"],
    visaStatusMode: "STRICT",
    visaStatuses: ["H-1B"],
  });

  const matching = {
    gothra: "Bharadwaja",
    faithTradition: "Vaishnava",
    faithSubTradition: "Sri Vaishnava",
    faithInstitution: "Local Samaj",
    nativeState: "Telangana",
    educationField: "Engineering",
    employerType: "Private",
    familyStatus: "Upper Middle Class",
    visaStatus: "H-1B",
  };

  assert.equal(strictPreferencesAllow(prefs, matching), true);
  assert.equal(preferredPreferenceFit(prefs, matching), 1);
  assert.equal(
    strictPreferencesAllow(prefs, { ...matching, gothra: "Kashyapa" }),
    false
  );
  assert.equal(
    strictPreferencesAllow(prefs, { ...matching, visaStatus: "Citizen" }),
    false
  );
  assert.equal(
    preferredPreferenceFit(prefs, { ...matching, employerType: "Government" }),
    5 / 6
  );
});

test("empty new list preferences normalize back to no preference", () => {
  const prefs = normalizePartnerPreferences({
    gothraMode: "STRICT",
    gothras: [],
    nativeStateMode: "PREFERRED",
    nativeStates: ["Telangana"],
    visaStatusMode: "STRICT",
    visaStatuses: [],
  });

  assert.equal(prefs.gothraMode, "NO_PREFERENCE");
  assert.equal(prefs.nativeStateMode, "PREFERRED");
  assert.deepEqual(prefs.nativeStates, ["Telangana"]);
  assert.equal(prefs.visaStatusMode, "NO_PREFERENCE");
});
