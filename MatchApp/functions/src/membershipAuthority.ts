import * as admin from "firebase-admin";

export type MembershipState = {
  active: boolean;
  planId: string;
  expiresAtMillis: number;
  paymentId: string;
  source: "PRIVATE_SUBSCRIPTION" | "LEGACY_PUBLIC_PROFILE" | "NONE";
};

function millis(value: unknown): number {
  if (value instanceof admin.firestore.Timestamp) return value.toMillis();
  const parsed = Number(value || 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

/**
 * Canonical paid-membership authority lives in subscriptions/{uid}. Legacy users may temporarily
 * fall back to historical public profile fields only so rollout can migrate without entitlement
 * loss. New writes must never repopulate those public billing fields.
 */
export function resolveMembershipState(
  subscription: FirebaseFirestore.DocumentData | undefined,
  legacyUser: FirebaseFirestore.DocumentData | undefined,
  nowMillis = Date.now()
): MembershipState {
  const privateExpiry = millis(subscription?.membershipUntilMillis);
  const privatePlan = typeof subscription?.membershipPlan === "string"
    ? subscription.membershipPlan.trim()
    : "";
  const privatePaymentId = typeof subscription?.membershipPaymentId === "string"
    ? subscription.membershipPaymentId.trim()
    : "";
  if (privateExpiry > 0 || privatePlan || privatePaymentId) {
    const active = subscription?.membershipActive === true &&
      privateExpiry > nowMillis &&
      privatePlan.length > 0 &&
      privatePaymentId.length > 0;
    return {
      active,
      planId: active ? privatePlan : "FREE",
      expiresAtMillis: active ? privateExpiry : 0,
      paymentId: active ? privatePaymentId : "",
      source: "PRIVATE_SUBSCRIPTION",
    };
  }

  const legacyExpiry = millis(legacyUser?.premiumUntil) ||
    millis(legacyUser?.subscriptionExpiry);
  const legacyPlan = String(
    legacyUser?.subscriptionPlan || legacyUser?.premiumPlan || ""
  ).trim();
  const legacyPaymentId = typeof legacyUser?.paymentId === "string"
    ? legacyUser.paymentId.trim()
    : "";
  const legacyActive = legacyUser?.isPremium === true &&
    legacyExpiry > nowMillis &&
    legacyPlan.length > 0 &&
    legacyPlan !== "FREE" &&
    legacyPaymentId.length > 0;
  if (legacyExpiry > 0 || legacyPlan || legacyPaymentId) {
    return {
      active: legacyActive,
      planId: legacyActive ? legacyPlan : "FREE",
      expiresAtMillis: legacyActive ? legacyExpiry : 0,
      paymentId: legacyActive ? legacyPaymentId : "",
      source: "LEGACY_PUBLIC_PROFILE",
    };
  }

  return {
    active: false,
    planId: "FREE",
    expiresAtMillis: 0,
    paymentId: "",
    source: "NONE",
  };
}
