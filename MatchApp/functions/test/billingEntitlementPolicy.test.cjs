const assert = require("node:assert/strict");
const test = require("node:test");

const {
  isVoidedPaymentStatus,
  rebuildEntitlementLedger,
} = require("../lib/billingEntitlementPolicy");

test("voided status classification is stable", () => {
  for (const status of ["VOIDED", "refunded", "Chargeback", "REVOKED", "canceled"]) {
    assert.equal(isVoidedPaymentStatus(status), true);
  }
  assert.equal(isVoidedPaymentStatus("ACTIVATED"), false);
});

test("stacked Play grants rebuild deterministically in grant order", () => {
  const result = rebuildEntitlementLedger([
    {
      id: "play_b",
      provider: "GOOGLE_PLAY",
      status: "ACTIVATED",
      durationMs: 50,
      grantedAtMillis: 120,
      entitlementId: "GOLD_6M",
    },
    {
      id: "play_a",
      provider: "GOOGLE_PLAY",
      status: "ACTIVATED",
      durationMs: 100,
      grantedAtMillis: 100,
      entitlementId: "SILVER_3M",
    },
  ], "");

  assert.deepEqual(result, {
    expiresAtMillis: 250,
    entitlementId: "GOLD_6M",
    paymentId: "play_b",
  });
});

test("refund removal rebuilds later surviving entitlement from its own grant time", () => {
  const contributions = [
    {
      id: "play_a",
      provider: "GOOGLE_PLAY",
      status: "ACTIVATED",
      durationMs: 100,
      grantedAtMillis: 100,
      entitlementId: "SILVER_3M",
    },
    {
      id: "play_b",
      provider: "GOOGLE_PLAY",
      status: "ACTIVATED",
      durationMs: 50,
      grantedAtMillis: 120,
      entitlementId: "GOLD_6M",
    },
  ];

  assert.deepEqual(rebuildEntitlementLedger(contributions, "play_a"), {
    expiresAtMillis: 170,
    entitlementId: "GOLD_6M",
    paymentId: "play_b",
  });
});

test("voided foreign and invalid contributions never extend entitlement", () => {
  const result = rebuildEntitlementLedger([
    {
      id: "voided",
      provider: "GOOGLE_PLAY",
      status: "VOIDED",
      durationMs: 1000,
      grantedAtMillis: 10,
      entitlementId: "PLATINUM_12M",
    },
    {
      id: "other-provider",
      provider: "OTHER",
      status: "ACTIVATED",
      durationMs: 1000,
      grantedAtMillis: 10,
      entitlementId: "PLATINUM_12M",
    },
    {
      id: "invalid-duration",
      provider: "GOOGLE_PLAY",
      status: "ACTIVATED",
      durationMs: 0,
      grantedAtMillis: 10,
      entitlementId: "PLATINUM_12M",
    },
  ], "");

  assert.deepEqual(result, {
    expiresAtMillis: 0,
    entitlementId: "",
    paymentId: "",
  });
});
