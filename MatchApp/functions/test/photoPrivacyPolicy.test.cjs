const assert = require("node:assert/strict");
const test = require("node:test");

const {
  normalizeProfilePhotoVisibility,
  canViewPublishedPhoto,
  canRequestPhotoAccess,
  photoAccessRequestMatchesParticipants,
} = require("../lib/photoPrivacyPolicy");

test("photo visibility normalization preserves safe supported values", () => {
  assert.equal(normalizeProfilePhotoVisibility(undefined), "PUBLIC");
  assert.equal(normalizeProfilePhotoVisibility("PUBLIC"), "PUBLIC");
  assert.equal(normalizeProfilePhotoVisibility("accepted_only"), "ACCEPTED_ONLY");
  assert.equal(normalizeProfilePhotoVisibility(" hidden "), "HIDDEN");
  assert.equal(normalizeProfilePhotoVisibility("unexpected"), "HIDDEN");
});

test("accepted-only photo visibility requires mutual interest or explicit grant", () => {
  assert.equal(canViewPublishedPhoto("PUBLIC", false, false), true);
  assert.equal(canViewPublishedPhoto("HIDDEN", true, true), false);
  assert.equal(canViewPublishedPhoto("ACCEPTED_ONLY", false, false), false);
  assert.equal(canViewPublishedPhoto("ACCEPTED_ONLY", true, false), true);
  assert.equal(canViewPublishedPhoto("ACCEPTED_ONLY", false, true), true);
});

test("photo access request is only available after requester interest and before access exists", () => {
  assert.equal(canRequestPhotoAccess("PUBLIC", true, false, false), false);
  assert.equal(canRequestPhotoAccess("HIDDEN", true, false, false), false);
  assert.equal(canRequestPhotoAccess("ACCEPTED_ONLY", false, false, false), false);
  assert.equal(canRequestPhotoAccess("ACCEPTED_ONLY", true, false, false), true);
  assert.equal(canRequestPhotoAccess("ACCEPTED_ONLY", true, true, false), false);
  assert.equal(canRequestPhotoAccess("ACCEPTED_ONLY", true, false, true), false);
});

test("colliding photo request keys cannot reveal status or overwrite another pair", () => {
  const existing = {
    requesterUid: "a_b",
    targetUid: "c",
    status: "PENDING",
  };
  // Both these UID pairs use the same legacy concatenated document ID: a_b_c.
  assert.equal(photoAccessRequestMatchesParticipants(existing, "a_b", "c"), true);
  assert.equal(photoAccessRequestMatchesParticipants(existing, "a", "b_c"), false);
  assert.equal(photoAccessRequestMatchesParticipants(existing, "c", "a_b"), false);
  assert.equal(photoAccessRequestMatchesParticipants({ ...existing, requesterUid: "" }, "a_b", "c"), false);
  assert.equal(photoAccessRequestMatchesParticipants(undefined, "a_b", "c"), false);
  assert.equal(photoAccessRequestMatchesParticipants(existing, "a_b", "a_b"), false);
});
