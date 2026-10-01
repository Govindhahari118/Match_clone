const assert = require("node:assert/strict");
const test = require("node:test");

const {
  normalizeAssistedPlan,
  normalizeAssistedStatus,
  assistedStatusTransitionAllowed,
} = require("../lib/assistedPolicy");

test("assisted service accepts only the published request plans", () => {
  assert.equal(normalizeAssistedPlan("Gold RM"), "Gold RM");
  assert.equal(normalizeAssistedPlan("VIP Unlimited"), null);
});

test("assisted request state machine prevents reopening terminal cases", () => {
  assert.equal(assistedStatusTransitionAllowed("OPEN", "ASSIGNED"), true);
  assert.equal(assistedStatusTransitionAllowed("ASSIGNED", "IN_PROGRESS"), true);
  assert.equal(assistedStatusTransitionAllowed("IN_PROGRESS", "RESOLVED"), true);
  assert.equal(assistedStatusTransitionAllowed("RESOLVED", "OPEN"), false);
  assert.equal(assistedStatusTransitionAllowed("CANCELLED", "ASSIGNED"), false);
  assert.equal(normalizeAssistedStatus("waiting_on_member"), "WAITING_ON_MEMBER");
});
