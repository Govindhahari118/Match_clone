const test = require("node:test");
const assert = require("node:assert/strict");

const {
  normalizeEnforcementStatus,
  enforcementSuppressesInteractions,
} = require("../lib/accountEnforcementPolicy");

test("normalizes supported moderation states", () => {
  assert.equal(normalizeEnforcementStatus(" active "), "ACTIVE");
  assert.equal(normalizeEnforcementStatus("under_review"), "UNDER_REVIEW");
  assert.equal(normalizeEnforcementStatus("restricted"), "RESTRICTED");
  assert.equal(normalizeEnforcementStatus("suspended"), "SUSPENDED");
  assert.equal(normalizeEnforcementStatus("deleted"), undefined);
});

test("only ACTIVE permits normal interactions", () => {
  assert.equal(enforcementSuppressesInteractions("ACTIVE"), false);
  assert.equal(enforcementSuppressesInteractions("UNDER_REVIEW"), true);
  assert.equal(enforcementSuppressesInteractions("RESTRICTED"), true);
  assert.equal(enforcementSuppressesInteractions("SUSPENDED"), true);
});
