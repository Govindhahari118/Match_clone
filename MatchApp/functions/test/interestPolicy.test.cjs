const assert = require("node:assert/strict");
const test = require("node:test");

const {
  declinedInterestAllowsNewRequest,
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
