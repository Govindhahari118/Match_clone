const assert = require("node:assert/strict");
const test = require("node:test");

const {
  activityVisibilityAllows,
  normalizeActivityVisibility,
} = require("../lib/activityVisibilityPolicy");

test("activity visibility defaults to mutual for missing or invalid settings", () => {
  assert.equal(normalizeActivityVisibility(undefined), "mutual");
  assert.equal(normalizeActivityVisibility("invalid"), "mutual");
});

test("everyone and nobody activity visibility are absolute", () => {
  const none = { interested: false, mutual: false };
  assert.equal(activityVisibilityAllows("everyone", none), true);
  assert.equal(activityVisibilityAllows("nobody", { interested: true, mutual: true }), false);
});

test("interest and mutual activity visibility require the matching relationship", () => {
  assert.equal(
    activityVisibilityAllows("interests", { interested: true, mutual: false }),
    true
  );
  assert.equal(
    activityVisibilityAllows("interests", { interested: false, mutual: false }),
    false
  );
  assert.equal(
    activityVisibilityAllows("mutual", { interested: true, mutual: false }),
    false
  );
  assert.equal(
    activityVisibilityAllows("mutual", { interested: true, mutual: true }),
    true
  );
});
