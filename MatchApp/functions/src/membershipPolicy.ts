import * as admin from "firebase-admin";

export type MembershipPlanId = "FREE" | "SILVER_3M" | "GOLD_6M" | "PLATINUM_12M";
export type MembershipCapability =
  | "ADVANCED_FILTERS"
  | "SEE_WHO_VIEWED"
  | "REVEAL_CONTACT"
  | "KUNDALI_MATCH"
  | "PROFILE_BOOST"
  | "VIDEO_CALL"
  | "RM_ASSISTANCE"
  | "PRIORITY_SUPPORT"
  | "READ_RECEIPTS"
  | "STEALTH_BROWSE";

const PAID_PLANS = new Set<MembershipPlanId>([
  "SILVER_3M",
  "GOLD_6M",
  "PLATINUM_12M",
]);

const CONTACT_LIMITS: Record<MembershipPlanId, number> = {
  FREE: 0,
  SILVER_3M: 75,
  GOLD_6M: 150,
  PLATINUM_12M: 300,
};

const CAPABILITIES: Record<MembershipCapability, ReadonlySet<MembershipPlanId>> = {
  ADVANCED_FILTERS: new Set(["SILVER_3M", "GOLD_6M", "PLATINUM_12M"]),
  SEE_WHO_VIEWED: new Set(["SILVER_3M", "GOLD_6M", "PLATINUM_12M"]),
  REVEAL_CONTACT: new Set(["SILVER_3M", "GOLD_6M", "PLATINUM_12M"]),
  KUNDALI_MATCH: new Set(["SILVER_3M", "GOLD_6M", "PLATINUM_12M"]),
  PROFILE_BOOST: new Set(["GOLD_6M", "PLATINUM_12M"]),
  VIDEO_CALL: new Set(["PLATINUM_12M"]),
  RM_ASSISTANCE: new Set(["PLATINUM_12M"]),
  PRIORITY_SUPPORT: new Set(["GOLD_6M", "PLATINUM_12M"]),
  READ_RECEIPTS: new Set(["GOLD_6M", "PLATINUM_12M"]),
  STEALTH_BROWSE: new Set(["GOLD_6M", "PLATINUM_12M"]),
};

function planId(value: unknown): MembershipPlanId {
  const normalized = String(value || "").trim().toUpperCase();
  return normalized === "SILVER_3M" ||
    normalized === "GOLD_6M" ||
    normalized === "PLATINUM_12M"
    ? normalized
    : "FREE";
}

export function membershipExpiryMillis(
  user: FirebaseFirestore.DocumentData
): number {
  if (user.premiumUntil instanceof admin.firestore.Timestamp) {
    return user.premiumUntil.toMillis();
  }
  const value = Number(user.subscriptionExpiry || 0);
  return Number.isFinite(value) ? value : 0;
}

/** Server truth for currently active paid membership. */
export function activeMembershipPlan(
  user: FirebaseFirestore.DocumentData,
  nowMillis = Date.now()
): MembershipPlanId {
  if (user.isPremium !== true || membershipExpiryMillis(user) <= nowMillis) {
    return "FREE";
  }
  const current = planId(user.subscriptionPlan || user.premiumPlan);
  return PAID_PLANS.has(current) ? current : "FREE";
}

export function hasMembershipCapability(
  user: FirebaseFirestore.DocumentData,
  capability: MembershipCapability,
  nowMillis = Date.now()
): boolean {
  return CAPABILITIES[capability].has(activeMembershipPlan(user, nowMillis));
}

export function contactRevealLimit(
  user: FirebaseFirestore.DocumentData,
  nowMillis = Date.now()
): number {
  return CONTACT_LIMITS[activeMembershipPlan(user, nowMillis)];
}

const ADVANCED_TEXT_FILTERS = [
  "caste", "subCaste", "incomeMin", "incomeMax", "educationLevel", "educationField",
  "occupationCategory", "employerType", "diet", "residentialStatus", "hasChildren",
  "hasChildrenFilter", "gothra", "nativeState", "countryOfResidence", "nriStatus",
  "smoking", "drinking", "familyType", "familyStatus", "physicalStatus", "citizenship",
  "rasi", "nakshatra", "manglik", "hobbies", "hasHoroscope",
] as const;

const ADVANCED_BOOLEAN_FILTERS = [
  "nriOnly", "willingToRelocate", "premiumOnly",
] as const;

const ADVANCED_NUMERIC_FILTERS = [
  "verifiedLevel", "recentlyJoinedDays", "lastActiveWithinDays", "minPoruthamScore",
] as const;

export function advancedDiscoveryFiltersRequested(data: unknown): boolean {
  if (!data || typeof data !== "object") return false;
  const value = data as Record<string, unknown>;
  if (ADVANCED_TEXT_FILTERS.some((key) =>
    typeof value[key] === "string" && value[key].trim().length > 0
  )) return true;
  if (ADVANCED_BOOLEAN_FILTERS.some((key) => value[key] === true)) return true;
  return ADVANCED_NUMERIC_FILTERS.some((key) => {
    const parsed = Number(value[key] || 0);
    return Number.isFinite(parsed) && parsed > 0;
  });
}
