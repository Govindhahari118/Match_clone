const assert = require("node:assert/strict");
const test = require("node:test");
const {
  PHOTO_REQUEST_COOLDOWN_MS,
  photoRequestCoolingDown,
  photoRequestNextAllowedAt,
} = require("../lib/photoRequestPolicy");

test("photo requests enforce an exact seven-day pair cooldown", () => {
  const now = 2_000_000_000_000;
  assert.equal(photoRequestCoolingDown(0, now), false);
  assert.equal(photoRequestCoolingDown(now - PHOTO_REQUEST_COOLDOWN_MS - 1, now), false);
  assert.equal(photoRequestCoolingDown(now - PHOTO_REQUEST_COOLDOWN_MS, now), false);
  assert.equal(photoRequestCoolingDown(now - PHOTO_REQUEST_COOLDOWN_MS + 1, now), true);
  assert.equal(photoRequestCoolingDown(now - 1_000, now), true);
});

test("photo request next-allowed timestamp is deterministic", () => {
  const last = 1_900_000_000_000;
  assert.equal(photoRequestNextAllowedAt(last), last + PHOTO_REQUEST_COOLDOWN_MS);
  assert.equal(photoRequestNextAllowedAt(0), 0);
});
