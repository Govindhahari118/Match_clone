const assert = require("node:assert/strict");
const test = require("node:test");

const {
  nearbyAccountIsActive,
  nearbyAccountIsDiscoverable,
} = require("../lib/nearbyPolicy");

test("active or legacy-active accounts may use Nearby", () => {
  for (const status of [undefined, "", "ACTIVE", "active"]) {
    assert.equal(nearbyAccountIsActive(status), true);
    assert.equal(nearbyAccountIsDiscoverable(status, false), true);
  }
});

test("suppressed account states cannot use or enter Nearby", () => {
  for (const status of ["PAUSED", "DELETING", "DELETED", "SUSPENDED"]) {
    assert.equal(nearbyAccountIsActive(status), false);
    assert.equal(nearbyAccountIsDiscoverable(status, false), false);
  }
});

test("stealth mode remains excluded from Nearby discovery", () => {
  assert.equal(nearbyAccountIsDiscoverable("ACTIVE", true), false);
  assert.equal(nearbyAccountIsDiscoverable(undefined, true), false);
});
