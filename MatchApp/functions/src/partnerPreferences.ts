import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";
import { requireActiveConsent } from "./consent";
import {
  DEFAULT_PARTNER_PREFERENCES,
  normalizePartnerPreferences,
} from "./partnerPreferencesPolicy";

function publicPayload(value: unknown) {
  const normalized = normalizePartnerPreferences(value);
  return {
    ...normalized,
    schemaVersion: 4,
  };
}

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
    : { ...DEFAULT_PARTNER_PREFERENCES, schemaVersion: 4 };
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
    schemaVersion: 4,
    createdAt: existing.data()?.createdAt || admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: false });

  return publicPayload({ ...normalized, configured: true });
});
