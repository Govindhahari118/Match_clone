import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";
import { requireActiveConsent } from "./consent";
import { accountIsActive } from "./accountStatusPolicy";
import { discoveryCandidateReady } from "./discoveryEligibilityPolicy";
import {
  DEFAULT_PARTNER_PREFERENCES,
  normalizePartnerPreferences,
  publicPartnerPreferenceSummary,
} from "./partnerPreferencesPolicy";

function publicPayload(value: unknown) {
  const normalized = normalizePartnerPreferences(value);
  return {
    ...normalized,
    schemaVersion: 5,
  };
}


/**
 * Returns only an explicitly shared, non-sensitive partner-expectation summary.
 * Religion/community/faith, income, complexion, physical status, citizenship and visa criteria
 * remain private even when sharing is enabled.
 */
export const getPartnerPreferenceSummary = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const requesterUid = context.auth?.uid;
  if (!requesterUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = typeof data?.targetUid === "string" ? data.targetUid.trim() : "";
  if (!targetUid || targetUid.length > 128 || targetUid === requesterUid) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target profile");
  }

  const [
    requester,
    target,
    preferences,
    requesterBlock,
    targetBlock,
    requesterRelation,
    targetRelation,
    reverseInterest,
  ] = await Promise.all([
    db.collection("users").doc(requesterUid).get(),
    db.collection("users").doc(targetUid).get(),
    db.collection("partnerPreferences").doc(targetUid).get(),
    db.collection("blocks").doc(requesterUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(requesterUid).get(),
    db.collection("privacyRelations").doc(requesterUid).collection("members").doc(targetUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(requesterUid).get(),
    db.collection("interests").doc(`${targetUid}_${requesterUid}`).get(),
  ]);

  if (!requester.exists || !target.exists ||
      !accountIsActive(requester.data()?.accountStatus) ||
      !accountIsActive(target.data()?.accountStatus) ||
      !discoveryCandidateReady(target.data() || {}, preferences.data())) {
    throw new functions.https.HttpsError("failed-precondition", "Profile is unavailable");
  }
  if (target.data()?.stealthMode === true && !reverseInterest.exists) {
    throw new functions.https.HttpsError("permission-denied", "Profile is unavailable");
  }
  if (requesterBlock.exists || targetBlock.exists ||
      requesterRelation.data()?.profileHidden === true ||
      targetRelation.data()?.profileHidden === true) {
    throw new functions.https.HttpsError("permission-denied", "Profile is unavailable");
  }

  return publicPartnerPreferenceSummary(preferences.exists ? preferences.data() : DEFAULT_PARTNER_PREFERENCES);
});

/**
 * Partner preferences are private account data. They are available only to the owner through this
 * authenticated callable; peer matching reads them inside trusted backend code.
 */
export const getPartnerPreferences = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const snapshot = await db.collection("partnerPreferences").doc(uid).get();
  return snapshot.exists
    ? publicPayload(snapshot.data())
    : { ...DEFAULT_PARTNER_PREFERENCES, schemaVersion: 5 };
});

/**
 * Replaces the current preference contract with sanitized values. A single canonical document
 * makes cross-device updates deterministic and avoids mixing discovery-session filters with durable
 * partner intent.
 */
export const setPartnerPreferences = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  await requireActiveConsent(uid, "sensitive_preferences");

  const normalized = normalizePartnerPreferences({
    ...(data && typeof data === "object" ? data : {}),
    configured: true,
  });
  const ref = db.collection("partnerPreferences").doc(uid);
  const existing = await ref.get();

  await ref.set({
    ...normalized,
    schemaVersion: 5,
    createdAt: existing.data()?.createdAt || admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: false });

  return publicPayload({ ...normalized, configured: true });
});
