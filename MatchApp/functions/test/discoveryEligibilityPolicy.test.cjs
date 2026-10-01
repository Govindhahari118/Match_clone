const test = require("node:test");
const assert = require("node:assert/strict");

const {
  publicDiscoveryProfileReady,
  partnerPreferencesReady,
  discoveryActorReady,
  discoveryCandidateReady,
} = require("../lib/discoveryEligibilityPolicy");

const complete = {
  username: "member123",
  displayName: "Member",
  age: 29,
  state: "Telangana",
  city: "Hyderabad",
  motherTongue: "Telugu",
  religion: "Hindu",
  education: "B.Tech",
  profession: "Engineer",
  occupationCategory: "Private Sector",
  maritalStatus: "Never Married",
  heightCm: 170,
  weight: 68,
};

test("public discovery profile gate requires all canonical profile fields", () => {
  assert.equal(publicDiscoveryProfileReady(complete), true);
  assert.equal(publicDiscoveryProfileReady({ ...complete, city: "" }), false);
  assert.equal(publicDiscoveryProfileReady({ ...complete, age: 17 }), false);
  assert.equal(publicDiscoveryProfileReady({ ...complete, username: "ab" }), false);
  assert.equal(publicDiscoveryProfileReady({ ...complete, weight: 0 }), false);
  assert.equal(publicDiscoveryProfileReady({ ...complete, occupationCategory: "" }), false);
});

test("discovery actor additionally requires DOB and configured preferences", () => {
  assert.equal(
    discoveryActorReady(complete, { dateOfBirth: "1997-01-01" }, { configured: true }),
    true
  );
  assert.equal(
    discoveryActorReady(complete, { dateOfBirth: "" }, { configured: true }),
    false
  );
  assert.equal(
    discoveryActorReady(complete, { dateOfBirth: "1997-01-01" }, { configured: false }),
    false
  );
});

test("candidate must have complete public profile and configured preferences", () => {
  assert.equal(discoveryCandidateReady(complete, { configured: true }), true);
  assert.equal(discoveryCandidateReady(complete, undefined), false);
  assert.equal(
    discoveryCandidateReady({ ...complete, profession: "" }, { configured: true }),
    false
  );
  assert.equal(partnerPreferencesReady({ configured: true }), true);
  assert.equal(partnerPreferencesReady(undefined), false);
});
