const assert = require("node:assert/strict");
const test = require("node:test");

const { computeTrustPolicy, normalizeRiskLevel } = require("../lib/trustPolicy");

test("trust score never contains a premium or payment input", () => {
  const result = computeTrustPolicy({
    phoneVerified: true,
    emailVerified: true,
    identityLevel: 2,
    profileCompleteness: 1,
    hasPhoto: true,
    accountAgeDays: 365,
    reviewedSafetyStanding: true,
    riskLevel: "LOW",
  });

  assert.equal(result.score, 100);
  assert.equal(result.tier, "HIGH");
  assert.equal(result.positiveFactors.some((value) => /premium|payment/i.test(value)), false);
});

test("reviewed risk can reduce public trust without exposing risk reasons", () => {
  const common = {
    phoneVerified: true,
    emailVerified: true,
    identityLevel: 2,
    profileCompleteness: 1,
    hasPhoto: true,
    accountAgeDays: 365,
    reviewedSafetyStanding: false,
  };

  const low = computeTrustPolicy({ ...common, riskLevel: "LOW" });
  const high = computeTrustPolicy({ ...common, riskLevel: "HIGH" });

  assert.equal(low.score, 90);
  assert.equal(high.score, 60);
  assert.equal(high.positiveFactors.some((value) => /risk|report|fraud|reason/i.test(value)), false);
});

test("trust policy clamps malformed inputs and normalizes risk", () => {
  const result = computeTrustPolicy({
    phoneVerified: false,
    emailVerified: false,
    identityLevel: 999,
    profileCompleteness: 9,
    hasPhoto: false,
    accountAgeDays: -100,
    reviewedSafetyStanding: false,
    riskLevel: normalizeRiskLevel("unexpected"),
  });

  assert.equal(result.score, 50);
  assert.equal(normalizeRiskLevel("critical"), "CRITICAL");
  assert.equal(normalizeRiskLevel(undefined), "LOW");
});
