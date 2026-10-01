const test = require("node:test");
const assert = require("node:assert/strict");

const { validateInterestIntro, MAX_INTEREST_INTRO_LENGTH } =
  require("../lib/interestIntroPolicy");

test("interest intro allows a short respectful note", () => {
  const result = validateInterestIntro("Hi, I liked your profile and would be happy to connect.");
  assert.equal(result.allowed, true);
  assert.equal(result.note, "Hi, I liked your profile and would be happy to connect.");
});

test("interest intro blocks contact and payment bypasses", () => {
  assert.equal(validateInterestIntro("WhatsApp me at +91 98765 43210").allowed, false);
  assert.equal(validateInterestIntro("email me at a@example.com").allowed, false);
  assert.equal(validateInterestIntro("see https://example.com").allowed, false);
  assert.equal(validateInterestIntro("please send money to my UPI id").allowed, false);
});

test("interest intro rejects content beyond the bounded length", () => {
  const result = validateInterestIntro("x".repeat(MAX_INTEREST_INTRO_LENGTH + 1));
  assert.equal(result.allowed, false);
  assert.equal(result.reason, "too_long");
});
