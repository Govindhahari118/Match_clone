import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";
import { productionFeatureEnabled } from "./featureFlagPolicy";
import { accountIsActive } from "./accountStatusPolicy";
import { evaluateCallEligibility } from "./callPolicy";

const COMMUNICATION_PROVIDER_IMPLEMENTED = false;

function cleanUid(value: unknown): string {
  const uid = typeof value === "string" ? value.trim() : "";
  if (!uid || uid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "A valid target member is required");
  }
  return uid;
}

function pairId(uidA: string, uidB: string): string {
  return [uidA, uidB].sort().join("_");
}

/**
 * Returns current relationship eligibility separately from provider availability.
 *
 * This endpoint is intentionally truthful: a valid mutual match is not the same thing as having
 * an audited voice/video provider. The production route stays disabled until a real provider
 * adapter, credentials, callbacks, rate limits and device evidence exist.
 */
export const getSecureCallCapability = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = context.auth?.uid;
  if (!callerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = cleanUid(data?.targetUid);
  const [
    caller,
    target,
    match,
    callerBlock,
    targetBlock,
    callerPrivacy,
    targetPrivacy,
  ] = await Promise.all([
    db.collection("users").doc(callerUid).get(),
    db.collection("users").doc(targetUid).get(),
    db.collection("matches").doc(pairId(callerUid, targetUid)).get(),
    db.collection("blocks").doc(callerUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(callerUid).get(),
    db.collection("privacyRelations").doc(callerUid).collection("members").doc(targetUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(callerUid).get(),
  ]);

  const result = evaluateCallEligibility({
    callerUid,
    targetUid,
    callerActive: caller.exists && accountIsActive(caller.data()?.accountStatus),
    targetActive: target.exists && accountIsActive(target.data()?.accountStatus),
    mutualMatch: match.exists &&
      Array.isArray(match.data()?.users) &&
      match.data()?.users.includes(callerUid) &&
      match.data()?.users.includes(targetUid),
    blockedEitherWay: callerBlock.exists || targetBlock.exists,
    profileHiddenEitherWay:
      callerPrivacy.data()?.profileHidden === true ||
      targetPrivacy.data()?.profileHidden === true,
    contactHiddenEitherWay:
      callerPrivacy.data()?.contactHidden === true ||
      targetPrivacy.data()?.contactHidden === true,
  });

  const rolloutEnabled = productionFeatureEnabled(functions.config().features?.secure_calls);
  const providerReady = COMMUNICATION_PROVIDER_IMPLEMENTED && rolloutEnabled;

  return {
    eligible: result.eligible,
    reason: result.reason,
    providerReady,
    rolloutEnabled,
    voiceAvailable: result.eligible && providerReady,
    videoAvailable: result.eligible && providerReady,
    numberMaskingAvailable: result.eligible && providerReady,
  };
});
