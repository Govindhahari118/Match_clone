const assert = require("node:assert/strict");
const test = require("node:test");

const {
  nearbyAccountIsDiscoverable,
} = require("../lib/nearbyPolicy");

test("active or legacy-active accounts may enter Nearby", () => {
  assert.equal(nearbyAccountIsDiscoverable(undefined, false), true);
  assert.equal(nearbyAccountIsDiscoverable("", false), true);
  assert.equal(nearbyAccountIsDiscoverable("ACTIVE", false), true);
  assert.equal(nearbyAccountIsDiscoverable("active", false), true);
});

test("suppressed account states never enter Nearby", () => {
  for (const status of ["PAUSED", "DELETING", "DELETED", "SUSPENDED"]) {
    assert.equal(nearbyAccountIsDiscoverable(status, false), false);
  }
});

test("stealth mode remains excluded from Nearby discovery", () => {
  assert.equal(nearbyAccountIsDiscoverable("ACTIVE", true), false);
  assert.equal(nearbyAccountIsDiscoverable(undefined, true), false);
});
