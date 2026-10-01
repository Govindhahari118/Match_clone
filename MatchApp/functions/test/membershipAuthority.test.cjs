const test = require("node:test");
const assert = require("node:assert/strict");
const { resolveMembershipState } = require("../lib/membershipAuthority");

test("private subscription membership is authoritative", () => {
  const now = 1_000_000;
  const state = resolveMembershipState({
    membershipActive: true,
    membershipPlan: "GOLD_6M",
    membershipUntilMillis: now + 1000,
    membershipPaymentId: "play_private",
  }, {
    isPremium: true,
    subscriptionPlan: "PLATINUM_12M",
    subscriptionExpiry: now + 999999,
    paymentId: "legacy_public",
  }, now);
  assert.deepEqual(state, {
    active: true,
    planId: "GOLD_6M",
    expiresAtMillis: now + 1000,
    paymentId: "play_private",
    source: "PRIVATE_SUBSCRIPTION",
  });
});

test("legacy public membership remains a temporary migration fallback", () => {
  const now = 1_000_000;
  const state = resolveMembershipState({}, {
    isPremium: true,
    subscriptionPlan: "SILVER_3M",
    subscriptionExpiry: now + 1000,
    paymentId: "play_legacy",
  }, now);
  assert.equal(state.active, true);
  assert.equal(state.planId, "SILVER_3M");
  assert.equal(state.source, "LEGACY_PUBLIC_PROFILE");
});

test("expired or incomplete membership fails closed", () => {
  const now = 1_000_000;
  assert.equal(resolveMembershipState({
    membershipActive: true,
    membershipPlan: "GOLD_6M",
    membershipUntilMillis: now - 1,
    membershipPaymentId: "play_x",
  }, {}, now).active, false);
  assert.equal(resolveMembershipState({
    membershipActive: true,
    membershipPlan: "GOLD_6M",
    membershipUntilMillis: now + 1000,
  }, {}, now).active, false);
});
