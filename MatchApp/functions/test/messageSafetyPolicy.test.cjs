const assert = require("node:assert/strict");
const test = require("node:test");

const { classifyMessageSafety } = require("../lib/messageSafetyPolicy");

test("detects external links without storing or interpreting destination", () => {
  assert.deepEqual(classifyMessageSafety("See https://example.test/profile"), {
    externalLink: true,
    moneyRequest: false,
  });
  assert.equal(classifyMessageSafety("Visit www.example.test").externalLink, true);
});

test("detects common money-request language as a review signal only", () => {
  assert.equal(classifyMessageSafety("Please transfer money to my bank account").moneyRequest, true);
  assert.equal(classifyMessageSafety("Send ₹ 5000 urgently").moneyRequest, true);
  assert.equal(classifyMessageSafety("My bank is near my office").moneyRequest, false);
});

test("ordinary conversation does not create either signal", () => {
  assert.deepEqual(classifyMessageSafety("Hello, how was your day?"), {
    externalLink: false,
    moneyRequest: false,
  });
  assert.deepEqual(classifyMessageSafety(null), {
    externalLink: false,
    moneyRequest: false,
  });
});
