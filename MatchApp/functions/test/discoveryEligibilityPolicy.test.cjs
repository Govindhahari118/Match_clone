const test = require("node:test");
const assert = require("node:assert/strict");

const {
  publicDiscoveryProfileReady,
  partnerPreferencesReady,
  discoveryActorReady,
  discoveryCandidateReady,
  profileFreshEnough,
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


test("discovery freshness suppresses stale profiles while allowing recently active members", () => {
  const day = 86_400_000;
  const now = 200 * day;
  assert.equal(profileFreshEnough(now - 10 * day, 0, now), true);
  assert.equal(profileFreshEnough(now - 100 * day, 0, now), false);
  assert.equal(profileFreshEnough(now - 200 * day, now - 2 * day, now), true);
  assert.equal(profileFreshEnough(0, 0, now), true);
});
