const assert = require("node:assert/strict");
const test = require("node:test");

const {
  GENERIC_GOVERNMENT_ID_LEVEL,
  verificationLevelForMethod,
  reviewerRequestedLevelIsValid,
} = require("../lib/verificationLevelPolicy");

test("generic government ID evidence grants only level 2", () => {
  assert.equal(GENERIC_GOVERNMENT_ID_LEVEL, 2);
  assert.equal(verificationLevelForMethod("GENERIC_GOVERNMENT_ID_REVIEW"), 2);
  assert.equal(reviewerRequestedLevelIsValid("GENERIC_GOVERNMENT_ID_REVIEW", 2), true);
  for (const level of [3, 4, 5]) {
    assert.equal(reviewerRequestedLevelIsValid("GENERIC_GOVERNMENT_ID_REVIEW", level), false);
  }
});

test("unknown evidence cannot grant a verification level", () => {
  assert.equal(verificationLevelForMethod("SELFIE_ONLY"), null);
  assert.equal(reviewerRequestedLevelIsValid("SELFIE_ONLY", 4), false);
});
