const test = require("node:test");
const assert = require("node:assert/strict");
const { userPauseTransition } = require("../lib/accountPausePolicy");

test("member can pause and resume only ordinary active or paused state", () => {
  assert.deepEqual(
    userPauseTransition("ACTIVE", "ACTIVE", true),
    { allowed: true, nextStatus: "PAUSED", paused: true }
  );
  assert.deepEqual(
    userPauseTransition("PAUSED", "ACTIVE", false),
    { allowed: true, nextStatus: "ACTIVE", paused: false }
  );
});

test("moderation status cannot be overridden by member resume", () => {
  for (const status of ["UNDER_REVIEW", "RESTRICTED", "SUSPENDED"]) {
    assert.deepEqual(
      userPauseTransition(status, status, false),
      { allowed: false, reason: "ENFORCEMENT" }
    );
  }
});

test("enforcement document blocks resume even if public status is stale ACTIVE", () => {
  assert.deepEqual(
    userPauseTransition("ACTIVE", "SUSPENDED", false),
    { allowed: false, reason: "ENFORCEMENT" }
  );
});

test("deleting and unknown states fail closed", () => {
  assert.deepEqual(
    userPauseTransition("DELETING", "ACTIVE", false),
    { allowed: false, reason: "DELETION" }
  );
  assert.deepEqual(
    userPauseTransition("MYSTERY", "ACTIVE", true),
    { allowed: false, reason: "INVALID_STATE" }
  );
});
