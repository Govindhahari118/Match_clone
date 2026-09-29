const assert = require("node:assert/strict");
const test = require("node:test");

const {
  CONSENT_PURPOSES,
  currentConsentVersion,
  isCurrentConsentRecord,
} = require("../lib/consent");

test("every supported consent purpose has a versioned notice contract", () => {
  assert.ok(CONSENT_PURPOSES.length >= 7);
  for (const purpose of CONSENT_PURPOSES) {
    assert.match(currentConsentVersion(purpose), /^\d{4}-\d{2}-\d{2}\.\d+$/);
  }
});

test("identity and Aadhaar consent are distinct purposes", () => {
  assert.ok(CONSENT_PURPOSES.includes("identity_verification"));
  assert.ok(CONSENT_PURPOSES.includes("aadhaar_offline_verification"));
  assert.notEqual("identity_verification", "aadhaar_offline_verification");
});


test("current consent requires both granted=true and the current notice version", () => {
  const purpose = "personalization";
  const version = currentConsentVersion(purpose);
  assert.equal(isCurrentConsentRecord(purpose, { granted: true, noticeVersion: version }), true);
  assert.equal(isCurrentConsentRecord(purpose, { granted: false, noticeVersion: version }), false);
  assert.equal(isCurrentConsentRecord(purpose, { granted: true, noticeVersion: "stale" }), false);
  assert.equal(isCurrentConsentRecord(purpose, undefined), false);
});
