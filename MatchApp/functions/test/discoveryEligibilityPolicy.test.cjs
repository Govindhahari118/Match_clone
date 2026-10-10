const test = require("node:test");
const assert = require("node:assert/strict");

const {
  publicDiscoveryProfileReady,
  partnerPreferencesReady,
  discoveryActorReady,
  discoveryCandidateReady,
  profileFreshEnough,
  normalizeStaleDiscoveryDays,
  DEFAULT_STALE_DISCOVERY_DAYS,
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


test("discovery freshness has explicit 89/90/91 day boundary semantics", () => {
  const day = 86_400_000;
  const now = 200 * day;
  assert.equal(profileFreshEnough(now - 89 * day, 0, now), true);
  assert.equal(profileFreshEnough(now - 90 * day, 0, now), true);
  assert.equal(profileFreshEnough(now - 91 * day, 0, now), false);
});

test("private activity supersedes creation age without exposing the timestamp", () => {
  const day = 86_400_000;
  const now = 500 * day;
  assert.equal(profileFreshEnough(now - 400 * day, now - 2 * day, now), true);
  assert.equal(profileFreshEnough(now - 400 * day, now - 120 * day, now), false);
});

test("legacy/future freshness anchors fail safely", () => {
  const day = 86_400_000;
  const now = 200 * day;
  assert.equal(profileFreshEnough(0, 0, now), true);
  assert.equal(profileFreshEnough(now + 365 * day, 0, now), true);
  assert.equal(profileFreshEnough(now - 400 * day, now + 365 * day, now), true);
});

test("stale threshold is product-configurable but clamped to safe bounds", () => {
  assert.equal(normalizeStaleDiscoveryDays(undefined), DEFAULT_STALE_DISCOVERY_DAYS);
  assert.equal(normalizeStaleDiscoveryDays("45"), 45);
  assert.equal(normalizeStaleDiscoveryDays(1), 14);
  assert.equal(normalizeStaleDiscoveryDays(9999), 365);
});

test("unset stale discovery configuration uses the default", () => {
  for (const value of [null, undefined, "", "   ", "not-a-number"]) {
    assert.equal(normalizeStaleDiscoveryDays(value), DEFAULT_STALE_DISCOVERY_DAYS);
  }
  assert.equal(normalizeStaleDiscoveryDays(null, 45), 45);
});

test("new profile creation supersedes an older stale activity record", () => {
  const day = 86_400_000;
  const now = 500 * day;
  assert.equal(profileFreshEnough(now - 2 * day, now - 200 * day, now), true);
  assert.equal(profileFreshEnough(now - 200 * day, now - 2 * day, now), true);
  assert.equal(profileFreshEnough(now - 200 * day, now - 150 * day, now), false);
});
