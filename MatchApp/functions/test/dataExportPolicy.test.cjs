const assert = require("node:assert/strict");
const test = require("node:test");

const {
  DATA_EXPORT_COOLDOWN_MS,
  dataExportRetryAfterMs,
} = require("../lib/dataExportPolicy");

test("data export cooldown is ten minutes and counts down deterministically", () => {
  assert.equal(DATA_EXPORT_COOLDOWN_MS, 10 * 60 * 1000);
  const now = 1_000_000;
  assert.equal(dataExportRetryAfterMs(now + DATA_EXPORT_COOLDOWN_MS, now), DATA_EXPORT_COOLDOWN_MS);
  assert.equal(dataExportRetryAfterMs(now + 1_234, now), 1_234);
});

test("expired or malformed export cooldown never blocks", () => {
  const now = 1_000_000;
  assert.equal(dataExportRetryAfterMs(now - 1, now), 0);
  assert.equal(dataExportRetryAfterMs(null, now), 0);
  assert.equal(dataExportRetryAfterMs("not-a-number", now), 0);
});
