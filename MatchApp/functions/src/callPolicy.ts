export type CallEligibilityInput = {
  callerUid: string;
  targetUid: string;
  callerActive: boolean;
  targetActive: boolean;
  mutualMatch: boolean;
  blockedEitherWay: boolean;
  profileHiddenEitherWay: boolean;
  contactHiddenEitherWay: boolean;
};

export type CallEligibilityResult = {
  eligible: boolean;
  reason:
    | "eligible"
    | "invalid_pair"
    | "inactive_account"
    | "not_mutual_match"
    | "blocked"
    | "privacy_restricted";
};

/**
 * Pure policy shared by provider capability checks and tests.
 *
 * A communications provider must never become the relationship-authority layer. Provider tokens
 * may be issued only after this policy succeeds using fresh server-side state.
 */
export function evaluateCallEligibility(input: CallEligibilityInput): CallEligibilityResult {
  if (!input.callerUid || !input.targetUid || input.callerUid === input.targetUid) {
    return { eligible: false, reason: "invalid_pair" };
  }
  if (!input.callerActive || !input.targetActive) {
    return { eligible: false, reason: "inactive_account" };
  }
  if (input.blockedEitherWay) {
    return { eligible: false, reason: "blocked" };
  }
  if (input.profileHiddenEitherWay || input.contactHiddenEitherWay) {
    return { eligible: false, reason: "privacy_restricted" };
  }
  if (!input.mutualMatch) {
    return { eligible: false, reason: "not_mutual_match" };
  }
  return { eligible: true, reason: "eligible" };
}
