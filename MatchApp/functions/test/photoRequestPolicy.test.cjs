const assert = require("node:assert/strict");
const test = require("node:test");

const {
  PHOTO_REQUEST_COOLDOWN_MS,
  PHOTO_REQUEST_DAILY_LIMIT,
  publicPhotoAvailable,
  photoRequestCooldownRemaining,
  photoRequestDailyAllowed,
} = require("../lib/photoRequestPolicy");

test("public photo availability requires a non-blank published pointer", () => {
  assert.equal(publicPhotoAvailable(undefined), false);
  assert.equal(publicPhotoAvailable(""), false);
  assert.equal(publicPhotoAvailable("   "), false);
  assert.equal(publicPhotoAvailable("photos/alice/main.jpg"), true);
});

test("photo request cooldown applies only to pending requests", () => {
  const now = 2_000_000_000_000;
  const requested = now - 60_000;
  assert.equal(
    photoRequestCooldownRemaining("PENDING", requested, now),
    PHOTO_REQUEST_COOLDOWN_MS - 60_000
  );
  assert.equal(photoRequestCooldownRemaining("FULFILLED", requested, now), 0);
  assert.equal(photoRequestCooldownRemaining("PENDING", now - PHOTO_REQUEST_COOLDOWN_MS, now), 0);
});

test("daily request policy enforces the exact ceiling", () => {
  assert.equal(photoRequestDailyAllowed(0), true);
  assert.equal(photoRequestDailyAllowed(PHOTO_REQUEST_DAILY_LIMIT - 1), true);
  assert.equal(photoRequestDailyAllowed(PHOTO_REQUEST_DAILY_LIMIT), false);
  assert.equal(photoRequestDailyAllowed(PHOTO_REQUEST_DAILY_LIMIT + 1), false);
});
