const assert = require("node:assert/strict");
const test = require("node:test");

const {
  calculateProfileCompletenessValue,
} = require("../lib/profileCompletenessPolicy");

test("profile completeness mirrors the 20-field wizard contract", () => {
  const publicProfile = {
    username: "anu_rao",
    displayName: "Anu",
    state: "Telangana",
    city: "Hyderabad",
    motherTongue: "Telugu",
    bio: "About me",
    religion: "Hindu",
    education: "B.Tech",
    profession: "Engineer",
    heightCm: 165,
    maritalStatus: "Never Married",
    familyType: "Nuclear",
    familyValues: "Moderate",
    diet: "Vegetarian",
    countryOfResidence: "India",
    employer: "Example",
    aboutFamily: "Family details",
    spokenLanguages: ["Telugu", "English"],
  };
  const privateProfile = {
    dateOfBirth: "1996-02-14",
    incomeBand: "10-15 LPA",
  };

  assert.equal(
    calculateProfileCompletenessValue(publicProfile, privateProfile),
    1
  );
});

test("profile completeness uses private fields without exposing their values", () => {
  const publicProfile = {
    username: "anu_rao",
    displayName: "Anu",
    state: "Telangana",
    city: "Hyderabad",
    motherTongue: "Telugu",
    bio: "About me",
    religion: "Hindu",
    education: "B.Tech",
    profession: "Engineer",
    heightCm: 165,
    maritalStatus: "Never Married",
    familyType: "Nuclear",
    familyValues: "Moderate",
    diet: "Vegetarian",
    countryOfResidence: "India",
    employer: "Example",
    aboutFamily: "Family details",
    spokenLanguages: "Telugu,English",
  };

  assert.equal(calculateProfileCompletenessValue(publicProfile, {}), 0.9);
  assert.equal(
    calculateProfileCompletenessValue(publicProfile, {
      dateOfBirth: "1996-02-14",
      incomeBand: "10-15 LPA",
    }),
    1
  );
});

test("height must be positive and blank values do not count", () => {
  assert.equal(
    calculateProfileCompletenessValue({
      username: "anu_rao",
      displayName: " ",
      heightCm: 0,
    }, {}),
    0.05
  );
});
