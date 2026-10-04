const assert = require("node:assert/strict");
const test = require("node:test");
const { normalizeReportReason } = require("../lib/reportReasonPolicy");

test("report reason policy accepts stable codes", () => {
  assert.equal(normalizeReportReason("HARASSMENT"), "HARASSMENT");
  assert.equal(normalizeReportReason("SCAM_OR_MONEY_REQUEST"), "SCAM_OR_MONEY_REQUEST");
});

test("report reason policy preserves legacy English compatibility", () => {
  assert.equal(normalizeReportReason("Fake profile"), "FAKE_PROFILE");
  assert.equal(normalizeReportReason("Spam or scam"), "SCAM_OR_MONEY_REQUEST");
  assert.equal(normalizeReportReason("Under age"), "UNDERAGE_CONCERN");
});

test("report reason policy rejects localized or arbitrary display text", () => {
  assert.equal(normalizeReportReason("వేధింపు"), null);
  assert.equal(normalizeReportReason("anything"), null);
  assert.equal(normalizeReportReason(""), null);
});
