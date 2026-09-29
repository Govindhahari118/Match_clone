const assert = require("node:assert/strict");
const test = require("node:test");

const {
  MAX_DAILY_INTERESTS_SAFETY,
  HIGH_VOLUME_INTEREST_SIGNAL_THRESHOLD,
  MAX_DAILY_REPORTS,
  MAX_DAILY_SUPPORT_TICKETS,
  safeUsageCount,
  usageAllowed,
  crossesThreshold,
} = require("../lib/abusePolicy");

test("hard safety ceilings stay separate from product quota", () => {
  assert.equal(MAX_DAILY_INTERESTS_SAFETY, 200);
  assert.equal(HIGH_VOLUME_INTEREST_SIGNAL_THRESHOLD, 50);
  assert.equal(MAX_DAILY_REPORTS, 25);
  assert.equal(MAX_DAILY_SUPPORT_TICKETS, 10);
});

test("usage counters normalize malformed values", () => {
  assert.equal(safeUsageCount(undefined), 0);
  assert.equal(safeUsageCount(-10), 0);
  assert.equal(safeUsageCount("4"), 4);
  assert.equal(safeUsageCount("bad"), 0);
});

test("ceilings and signal threshold are deterministic", () => {
  assert.equal(usageAllowed(199, 200), true);
  assert.equal(usageAllowed(200, 200), false);
  assert.equal(crossesThreshold(48, 50), false);
  assert.equal(crossesThreshold(49, 50), true);
  assert.equal(crossesThreshold(50, 50), false);
});
