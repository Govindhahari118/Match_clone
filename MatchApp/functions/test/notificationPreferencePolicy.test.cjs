const test = require("node:test");
const assert = require("node:assert/strict");

const { quietHoursActive } = require("../lib/notificationPreferencePolicy");

test("quiet hours use the member timezone and fixed 22:00-07:00 window", () => {
  assert.equal(
    quietHoursActive(true, "Asia/Kolkata", new Date("2026-10-01T17:30:00.000Z")),
    true
  ); // 23:00 IST
  assert.equal(
    quietHoursActive(true, "Asia/Kolkata", new Date("2026-10-01T06:30:00.000Z")),
    false
  ); // 12:00 IST
  assert.equal(
    quietHoursActive(false, "Asia/Kolkata", new Date("2026-10-01T17:30:00.000Z")),
    false
  );
  assert.equal(
    quietHoursActive(true, "not/a-zone", new Date("2026-10-01T17:30:00.000Z")),
    false
  );
});
