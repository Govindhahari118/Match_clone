const test = require("node:test");
const assert = require("node:assert/strict");

const {
  PROFILE_MUTATION_REVIEW_THRESHOLD_PER_DAY,
  deviceAccountSwitchIsReviewSignal,
  profileMutationFieldsChanged,
  playAccountOwnershipMismatch,
} = require("../lib/riskSignalPolicy");

test("shared-device account switching is review-only signalable", () => {
  assert.equal(deviceAccountSwitchIsReviewSignal("alice", "bob"), true);
  assert.equal(deviceAccountSwitchIsReviewSignal("alice", "alice"), false);
  assert.equal(deviceAccountSwitchIsReviewSignal(undefined, "alice"), false);
});

test("profile mutation policy ignores server-only fields", () => {
  assert.equal(
    profileMutationFieldsChanged(
      { displayName: "Alice", subscriptionPlan: "FREE" },
      { displayName: "Alice", subscriptionPlan: "GOLD" }
    ),
    false
  );
  assert.equal(
    profileMutationFieldsChanged(
      { displayName: "Alice", city: "Hyderabad" },
      { displayName: "Alice", city: "Warangal" }
    ),
    true
  );
  assert.equal(PROFILE_MUTATION_REVIEW_THRESHOLD_PER_DAY, 20);
});

test("Play ownership mismatch is explicit and fail-closed", () => {
  assert.equal(playAccountOwnershipMismatch("hash-a", "hash-a"), false);
  assert.equal(playAccountOwnershipMismatch("hash-a", "hash-b"), true);
  assert.equal(playAccountOwnershipMismatch("hash-a", undefined), true);
});
