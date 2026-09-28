export type EntitlementContribution = {
  id: string;
  provider: string;
  status: unknown;
  durationMs: number;
  grantedAtMillis: number;
  entitlementId: string;
};

export type RebuiltEntitlement = {
  expiresAtMillis: number;
  entitlementId: string;
  paymentId: string;
};

export function isVoidedPaymentStatus(status: unknown): boolean {
  return ["VOIDED", "REFUNDED", "CHARGEBACK", "REVOKED", "CANCELED"]
    .includes(String(status || "").toUpperCase());
}

/**
 * Deterministically rebuilds a stacked one-time entitlement from its surviving grant
 * contributions. Sorting by grant time + id makes retries and out-of-order webhook delivery
 * converge to the same result.
 */
export function rebuildEntitlementLedger(
  contributions: EntitlementContribution[],
  excludedPaymentId: string
): RebuiltEntitlement {
  const entries = contributions
    .filter((entry) => entry.id !== excludedPaymentId)
    .filter((entry) =>
      entry.provider === "GOOGLE_PLAY" &&
      !isVoidedPaymentStatus(entry.status) &&
      Number.isFinite(entry.durationMs) &&
      entry.durationMs > 0 &&
      Number.isFinite(entry.grantedAtMillis) &&
      entry.grantedAtMillis > 0
    )
    .sort((a, b) =>
      a.grantedAtMillis - b.grantedAtMillis || a.id.localeCompare(b.id)
    );

  let cursor = 0;
  let entitlementId = "";
  let paymentId = "";
  for (const entry of entries) {
    const start = Math.max(entry.grantedAtMillis, cursor);
    cursor = start + entry.durationMs;
    entitlementId = entry.entitlementId;
    paymentId = entry.id;
  }
  return { expiresAtMillis: cursor, entitlementId, paymentId };
}
