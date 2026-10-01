const test = require("node:test");
const assert = require("node:assert/strict");
const { productionFeatureEnabled } = require("../lib/featureFlagPolicy");

test("production feature flag enables only explicit string true", () => {
  assert.equal(productionFeatureEnabled("true"), true);
  assert.equal(productionFeatureEnabled(" TRUE "), true);
  assert.equal(productionFeatureEnabled(true), false);
  assert.equal(productionFeatureEnabled("1"), false);
  assert.equal(productionFeatureEnabled(undefined), false);
});
