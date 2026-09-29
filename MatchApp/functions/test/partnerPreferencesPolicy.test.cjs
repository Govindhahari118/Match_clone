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
