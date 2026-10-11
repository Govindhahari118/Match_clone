const assert = require("node:assert/strict");
const test = require("node:test");

const {
  declinedInterestAllowsNewRequest,
  directedInterestMatches,
  matchMembersExactly,
} = require("../lib/interestPolicy");

test("a declined interest cannot be immediately recreated by the sender", () => {
  assert.equal(declinedInterestAllowsNewRequest("declined", false), false);
  assert.equal(declinedInterestAllowsNewRequest("DECLINED", false), false);
});

test("recipient initiation can reopen a previously declined pair", () => {
  assert.equal(declinedInterestAllowsNewRequest("declined", true), true);
});

test("no decline response leaves the normal request path available", () => {
  assert.equal(declinedInterestAllowsNewRequest("", false), true);
  assert.equal(declinedInterestAllowsNewRequest(undefined, false), true);
});

test("legacy colliding interest keys cannot create false reciprocal interests", () => {
  const written = { fromUid: "a_b", toUid: "c" };
  assert.equal(directedInterestMatches(written, "a_b", "c"), true);
  assert.equal(directedInterestMatches(written, "a", "b_c"), false);
  assert.equal(directedInterestMatches(written, "c", "a_b"), false);
  assert.equal(directedInterestMatches({ fromUid: "a_b" }, "a_b", "c"), false);
  assert.equal(directedInterestMatches(undefined, "a_b", "c"), false);
});

test("legacy colliding match IDs cannot overwrite a different pair", () => {
  const stored = { users: ["a_b", "c"] };
  assert.equal(matchMembersExactly(stored, "a_b", "c"), true);
  assert.equal(matchMembersExactly(stored, "c", "a_b"), true);
  assert.equal(matchMembersExactly(stored, "a", "b_c"), false);
  assert.equal(matchMembersExactly({ users: ["a_b", "a_b"] }, "a_b", "c"), false);
  assert.equal(matchMembersExactly({ users: ["a_b", "c", "other"] }, "a_b", "c"), false);
});
