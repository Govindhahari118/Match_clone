const assert = require("node:assert/strict");
const test = require("node:test");

const {
  deletionBatchCount,
  shouldReassertPublicSuppression,
} = require("../lib/deletionPolicy");

test("public suppression is reasserted until public profile deletion is checkpointed", () => {
  assert.equal(shouldReassertPublicSuppression([]), true);
  assert.equal(
    shouldReassertPublicSuppression(["RELATIONSHIPS", "MEDIA"]),
    true
  );
  assert.equal(
    shouldReassertPublicSuppression(["RELATIONSHIPS", "PUBLIC_PROFILE"]),
    false
  );
});

test("retry after public-profile deletion never recreates users uid document", () => {
  const completedAfterAuthFailure = new Set([
    "RELATIONSHIPS",
    "ACTIVITY_AND_SERVICES",
    "OWNER_SCOPED_DATA",
    "CHATS",
    "MEDIA",
    "PRIVATE_SINGLETONS",
    "IDENTITY_REGISTRY",
    "PUBLIC_PROFILE",
  ]);
  assert.equal(
    shouldReassertPublicSuppression(completedAfterAuthFailure),
    false
  );
});

test("large accounts are processed in bounded retry-safe batches", () => {
  assert.equal(deletionBatchCount(0, 300), 0);
  assert.equal(deletionBatchCount(1, 300), 1);
  assert.equal(deletionBatchCount(300, 300), 1);
  assert.equal(deletionBatchCount(301, 300), 2);
  assert.equal(deletionBatchCount(501, 300), 2);
  assert.equal(deletionBatchCount(901, 300), 4);
  assert.throws(() => deletionBatchCount(10, 0), /positive/);
});
