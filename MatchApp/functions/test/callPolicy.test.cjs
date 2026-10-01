const assert = require("node:assert/strict");
const test = require("node:test");

const { evaluateCallEligibility } = require("../lib/callPolicy");

const base = {
  callerUid: "a",
  targetUid: "b",
  callerActive: true,
  targetActive: true,
  mutualMatch: true,
  blockedEitherWay: false,
  profileHiddenEitherWay: false,
  contactHiddenEitherWay: false,
};

test("secure calling requires an active mutual pair", () => {
  assert.deepEqual(evaluateCallEligibility(base), { eligible: true, reason: "eligible" });
  assert.equal(evaluateCallEligibility({ ...base, mutualMatch: false }).reason, "not_mutual_match");
  assert.equal(evaluateCallEligibility({ ...base, targetActive: false }).reason, "inactive_account");
});

test("blocks and member privacy override a mutual match", () => {
  assert.equal(evaluateCallEligibility({ ...base, blockedEitherWay: true }).reason, "blocked");
  assert.equal(
    evaluateCallEligibility({ ...base, profileHiddenEitherWay: true }).reason,
    "privacy_restricted"
  );
  assert.equal(
    evaluateCallEligibility({ ...base, contactHiddenEitherWay: true }).reason,
    "privacy_restricted"
  );
});

test("self calls and malformed pairs are never eligible", () => {
  assert.equal(evaluateCallEligibility({ ...base, targetUid: "a" }).reason, "invalid_pair");
  assert.equal(evaluateCallEligibility({ ...base, callerUid: "" }).reason, "invalid_pair");
});
