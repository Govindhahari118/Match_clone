const assert = require("node:assert/strict");
const test = require("node:test");

const {
  normalizeCallKind,
  canTransitionCallState,
  callInitiationAllowed,
} = require("../lib/callSessionPolicy");

test("call kind defaults safely to voice", () => {
  assert.equal(normalizeCallKind("VIDEO"), "VIDEO");
  assert.equal(normalizeCallKind("video"), "VIDEO");
  assert.equal(normalizeCallKind("VOICE"), "VOICE");
  assert.equal(normalizeCallKind("unexpected"), "VOICE");
  assert.equal(normalizeCallKind(undefined), "VOICE");
});

test("call lifecycle permits only forward security-safe transitions", () => {
  assert.equal(canTransitionCallState("REQUESTED", "RINGING"), true);
  assert.equal(canTransitionCallState("REQUESTED", "CONNECTED"), false);
  assert.equal(canTransitionCallState("RINGING", "CONNECTED"), true);
  assert.equal(canTransitionCallState("CONNECTED", "ENDED"), true);
  assert.equal(canTransitionCallState("ENDED", "CONNECTED"), false);
  assert.equal(canTransitionCallState("REJECTED", "RINGING"), false);
  assert.equal(canTransitionCallState("FAILED", "REQUESTED"), false);
  assert.equal(canTransitionCallState("CONNECTED", "CONNECTED"), true);
});

test("call initiation rate policy enforces both account and pair ceilings", () => {
  assert.equal(callInitiationAllowed(0, 0, 10, 3), true);
  assert.equal(callInitiationAllowed(9, 2, 10, 3), true);
  assert.equal(callInitiationAllowed(10, 0, 10, 3), false);
  assert.equal(callInitiationAllowed(0, 3, 10, 3), false);
});
