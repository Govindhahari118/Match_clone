const assert = require("node:assert/strict");
const test = require("node:test");

const { accountIsActive } = require("../lib/accountStatusPolicy");

test("legacy empty and ACTIVE account states are active", () => {
  for (const status of [undefined, null, "", "ACTIVE", "active", " Active "]) {
    assert.equal(accountIsActive(status), true);
  }
});

test("suppressed lifecycle states are not active", () => {
  for (const status of ["PAUSED", "DELETING", "DELETED", "SUSPENDED", "CLOSED"]) {
    assert.equal(accountIsActive(status), false);
  }
});
