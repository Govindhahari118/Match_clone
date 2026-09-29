export type TrustRiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type TrustInputs = {
  phoneVerified: boolean;
  emailVerified: boolean;
  identityLevel: number;
  profileCompleteness: number;
  hasPhoto: boolean;
  accountAgeDays: number;
  reviewedSafetyStanding: boolean;
  riskLevel: TrustRiskLevel;
};

export type TrustPolicyResult = {
  score: number;
  tier: "BASIC" | "BUILDING" | "STRONG" | "HIGH";
  positiveFactors: string[];
};

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}

export function normalizeRiskLevel(value: unknown): TrustRiskLevel {
  const normalized = typeof value === "string" ? value.trim().toUpperCase() : "";
  if (normalized === "MEDIUM" || normalized === "HIGH" || normalized === "CRITICAL") {
    return normalized;
  }
  return "LOW";
}

/**
 * Public Trust Score policy.
 *
 * Premium/payment status is deliberately absent. Fraud/risk reasons are deliberately absent from
 * the returned factors so attackers cannot reverse engineer moderation signals. Only an
 * operations-reviewed risk level may reduce the score.
 */
export function computeTrustPolicy(input: TrustInputs): TrustPolicyResult {
  let score = 0;
  const positiveFactors: string[] = [];

  const identityLevel = clamp(Math.trunc(input.identityLevel || 0), 0, 5);
  if (identityLevel >= 2) {
    score += 30;
    positiveFactors.push("Government identity reviewed");
  } else if (identityLevel >= 1) {
    score += 10;
    positiveFactors.push("Basic identity check completed");
  }

  if (input.phoneVerified) {
    score += 10;
    positiveFactors.push("Phone verified");
  }
  if (input.emailVerified) {
    score += 5;
    positiveFactors.push("Email verified");
  }

  const completeness = clamp(Number(input.profileCompleteness) || 0, 0, 1);
  score += Math.round(completeness * 20);
  if (completeness >= 0.8) positiveFactors.push("Profile is substantially complete");

  if (input.hasPhoto) {
    score += 10;
    positiveFactors.push("Profile photo added");
  }

  const ageDays = Math.max(0, Math.floor(Number(input.accountAgeDays) || 0));
  if (ageDays >= 180) {
    score += 15;
    positiveFactors.push("Established account history");
  } else if (ageDays >= 30) {
    score += 10;
    positiveFactors.push("Account history established");
  } else if (ageDays >= 7) {
    score += 5;
  }

  if (input.reviewedSafetyStanding) {
    score += 10;
    positiveFactors.push("Safety review in good standing");
  }

  const penalty = input.riskLevel === "CRITICAL" ? 45 :
    input.riskLevel === "HIGH" ? 30 :
    input.riskLevel === "MEDIUM" ? 15 : 0;
  score = clamp(Math.round(score - penalty), 0, 100);

  const tier = score >= 90 ? "HIGH" :
    score >= 75 ? "STRONG" :
    score >= 50 ? "BUILDING" : "BASIC";

  return { score, tier, positiveFactors };
}
