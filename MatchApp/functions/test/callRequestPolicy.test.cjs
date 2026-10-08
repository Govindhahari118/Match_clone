const assert = require("node:assert/strict");
const test = require("node:test");
const {
  normalizeCallRequestStatus,
  validProposedCallTime,
  canReplaceCallRequest,
  canRespondToCallRequest,
  canCancelCallRequest,
} = require("../lib/callRequestPolicy");

test("proposed call time stays inside the safe scheduling window", () => {
  const now = 1_000_000_000;
  assert.equal(validProposedCallTime(now, now + 14 * 60_000), false);
  assert.equal(validProposedCallTime(now, now + 15 * 60_000), true);
  assert.equal(validProposedCallTime(now, now + 14 * 24 * 60 * 60_000), true);
  assert.equal(validProposedCallTime(now, now + 14 * 24 * 60 * 60_000 + 1), false);
  assert.equal(validProposedCallTime(now, Number.NaN), false);
});

test("active requests cannot be spam-replaced inside six hours", () => {
  const now = 10_000_000;
  assert.equal(canReplaceCallRequest(null, 0, now), true);
  assert.equal(canReplaceCallRequest("DECLINED", now - 1, now), true);
  assert.equal(canReplaceCallRequest("PENDING", now - 5 * 60 * 60_000, now), false);
  assert.equal(canReplaceCallRequest("PENDING", now - 6 * 60 * 60_000, now), true);
  assert.equal(canReplaceCallRequest("ACCEPTED", now - 5 * 60 * 60_000, now), false);
});

test("only target may respond and only requester may cancel", () => {
  assert.equal(canRespondToCallRequest("PENDING", "b", "b"), true);
  assert.equal(canRespondToCallRequest("PENDING", "a", "b"), false);
  assert.equal(canRespondToCallRequest("ACCEPTED", "b", "b"), false);
  assert.equal(canCancelCallRequest("PENDING", "a", "a"), true);
  assert.equal(canCancelCallRequest("ACCEPTED", "a", "a"), true);
  assert.equal(canCancelCallRequest("DECLINED", "a", "a"), false);
});

test("status normalization rejects unknown states", () => {
  assert.equal(normalizeCallRequestStatus("pending"), "PENDING");
  assert.equal(normalizeCallRequestStatus("accepted"), "ACCEPTED");
  assert.equal(normalizeCallRequestStatus("weird"), null);
});

test("invalid time inputs fail closed", () => {
  const now = 1_000_000_000;
  assert.equal(validProposedCallTime(Number.NaN, now + 60 * 60_000), false);
  assert.equal(validProposedCallTime(Infinity, now + 60 * 60_000), false);
  assert.equal(canReplaceCallRequest("PENDING", Number.NaN, now), false);
  assert.equal(canReplaceCallRequest("ACCEPTED", Infinity, now), false);
  assert.equal(canReplaceCallRequest(null, 0, Number.NaN), false);
});

test("empty participant identifiers never authorize transitions", () => {
  assert.equal(canRespondToCallRequest("PENDING", "", ""), false);
  assert.equal(canCancelCallRequest("PENDING", "", ""), false);
  assert.equal(canCancelCallRequest("ACCEPTED", "", ""), false);
});
