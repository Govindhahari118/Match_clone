const assert = require("node:assert/strict");
const test = require("node:test");

const {
  calculateProfileCompletenessValue,
} = require("../lib/profileCompletenessPolicy");

function completePublicProfile() {
  return {
    username: "anu_rao",
    displayName: "Anu",
    state: "Telangana",
    city: "Hyderabad",
    motherTongue: "Telugu",
    religion: "Hindu",
    heightCm: 165,
    maritalStatus: "Never Married",
    photoUrl: "photos/alice/approved.jpg",
    education: "B.Tech",
    profession: "Engineer",
    employer: "Example",
    familyType: "Nuclear",
    familyValues: "Moderate",
    aboutFamily: "Family details",
    fatherOccupation: "Retired",
    diet: "Vegetarian",
    smoking: "Never",
    drinking: "Never",
    hobbies: ["Reading"],
    spokenLanguages: ["Telugu", "English"],
    bio: "About me",
    isVerified: true,
    verificationLevel: 2,
  };
}

test("weighted profile strength reaches 100% only across high-value sections", () => {
  assert.equal(
    calculateProfileCompletenessValue(
      completePublicProfile(),
      {
        dateOfBirth: "1996-02-14",
        incomeBand: "10-15 LPA",
      },
      { configured: true },
      { phoneStatus: "VERIFIED" }
    ),
    1
  );
});

test("partner preference and verification sections are deliberate weighted evidence", () => {
  const profile = completePublicProfile();
  assert.equal(
    calculateProfileCompletenessValue(
      profile,
      {
        dateOfBirth: "1996-02-14",
        incomeBand: "10-15 LPA",
      },
      {},
      {}
    ),
    0.77
  );
  assert.equal(
    calculateProfileCompletenessValue(
      profile,
      {
        dateOfBirth: "1996-02-14",
        incomeBand: "10-15 LPA",
      },
      { configured: true },
      { phoneStatus: "VERIFIED" }
    ),
    1
  );
});

test("explicit no-preference configuration completes the partner-intent section", () => {
  const base = calculateProfileCompletenessValue({}, {}, {}, {});
  const configured = calculateProfileCompletenessValue(
    {},
    {},
    { configured: true, cityMode: "NO_PREFERENCE" },
    {}
  );
  assert.equal(base, 0);
  assert.equal(configured, 0.15);
});

test("single basic field earns only its fraction of the Basics section", () => {
  assert.equal(
    calculateProfileCompletenessValue({ username: "anu_rao" }, {}, {}, {}),
    0.017
  );
});
