const test = require("node:test");
const assert = require("node:assert/strict");

const {
  quietHoursActive,
  normalizeQuietHour,
  QUIET_HOURS_START,
  QUIET_HOURS_END,
} = require("../lib/notificationPreferencePolicy");

test("quiet hours use the member timezone and default 22:00-07:00 window", () => {
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
});

test("quiet hours handle both overnight and same-day configurable windows", () => {
  // 23:00 IST is inside a custom overnight 21:00-06:00 window.
  assert.equal(
    quietHoursActive(true, "Asia/Kolkata", new Date("2026-10-01T17:30:00.000Z"), 21, 6),
    true
  );
  // 13:00 IST is inside a same-day 12:00-14:00 window.
  assert.equal(
    quietHoursActive(true, "Asia/Kolkata", new Date("2026-10-01T07:30:00.000Z"), 12, 14),
    true
  );
  assert.equal(
    quietHoursActive(true, "Asia/Kolkata", new Date("2026-10-01T09:30:00.000Z"), 12, 14),
    false
  );
});

test("invalid timezone/date fail open and invalid hours fall back safely", () => {
  assert.equal(
    quietHoursActive(true, "not/a-zone", new Date("2026-10-01T17:30:00.000Z")),
    false
  );
  assert.equal(quietHoursActive(true, "Asia/Kolkata", new Date("invalid")), false);
  assert.equal(normalizeQuietHour(-1, QUIET_HOURS_START), QUIET_HOURS_START);
  assert.equal(normalizeQuietHour(24, QUIET_HOURS_END), QUIET_HOURS_END);
  assert.equal(normalizeQuietHour("6", QUIET_HOURS_END), 6);
});

test("equal quiet-hour boundaries do not mute all optional push delivery", () => {
  assert.equal(
    quietHoursActive(true, "Asia/Kolkata", new Date("2026-10-01T17:30:00.000Z"), 7, 7),
    false
  );
});
