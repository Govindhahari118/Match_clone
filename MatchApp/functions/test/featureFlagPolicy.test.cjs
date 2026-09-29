const test = require("node:test");
const assert = require("node:assert/strict");
const { productionFeatureEnabled } = require("../lib/featureFlagPolicy");

test("production feature flags fail closed unless explicitly true", () => {
  assert.equal(productionFeatureEnabled(undefined), false);
  assert.equal(productionFeatureEnabled(null), false);
  assert.equal(productionFeatureEnabled(false), false);
  assert.equal(productionFeatureEnabled("false"), false);
  assert.equal(productionFeatureEnabled("1"), false);
  assert.equal(productionFeatureEnabled(" true "), true);
  assert.equal(productionFeatureEnabled("TRUE"), true);
});
