const test = require("node:test");
const assert = require("node:assert/strict");
const {
  GENERIC_GOVERNMENT_ID_LEVEL,
  verificationLevelAfterGenericReview,
} = require("../lib/verificationLevelPolicy");

test("generic government ID approval grants deterministic level 2", () => {
  assert.equal(GENERIC_GOVERNMENT_ID_LEVEL, 2);
  assert.equal(verificationLevelAfterGenericReview(0, true), 2);
  assert.equal(verificationLevelAfterGenericReview(1, true), 2);
});

test("generic government ID review never downgrades stronger provider evidence", () => {
  assert.equal(verificationLevelAfterGenericReview(4, true), 4);
  assert.equal(verificationLevelAfterGenericReview(5, false), 5);
});

test("malformed previous levels are clamped safely", () => {
  assert.equal(verificationLevelAfterGenericReview("invalid", true), 2);
  assert.equal(verificationLevelAfterGenericReview(99, false), 5);
  assert.equal(verificationLevelAfterGenericReview(-3, false), 0);
});
