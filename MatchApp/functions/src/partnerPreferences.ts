import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";
import { requireActiveConsent } from "./consent";
import { accountIsActive } from "./accountStatusPolicy";
import {
  DEFAULT_PARTNER_PREFERENCES,
  normalizePartnerPreferences,
} from "./partnerPreferencesPolicy";

function publicPayload(value: unknown) {
  const normalized = normalizePartnerPreferences(value);
  return {
    ...normalized,
    schemaVersion: 5,
  };
}


type PublicPreferenceSummaryItem = {
  key: string;
  mode: "STRICT" | "PREFERRED";
  value: string;
};

function publicSummary(value: unknown): { shared: boolean; items: PublicPreferenceSummaryItem[] } {
  const p = normalizePartnerPreferences(value);
  if (!p.configured || !p.sharePublicSummary) return { shared: false, items: [] };

  const items: PublicPreferenceSummaryItem[] = [];
  const addRange = (
    key: string,
    prefMode: "STRICT" | "PREFERRED" | "NO_PREFERENCE",
    min: number,
    max: number,
    suffix = ""
  ) => {
    if (prefMode === "NO_PREFERENCE") return;
    items.push({ key, mode: prefMode, value: `${min}–${max}${suffix}` });
  };
  const addList = (
    key: string,
    prefMode: "STRICT" | "PREFERRED" | "NO_PREFERENCE",
    values: string[]
  ) => {
    if (prefMode === "NO_PREFERENCE" || values.length === 0) return;
    items.push({
      key,
      mode: prefMode,
      value: values.slice(0, 4).join(", "),
    });
  };

  addRange("age", p.ageMode, p.ageMin, p.ageMax);
  addRange("height", p.heightMode, p.heightMinCm, p.heightMaxCm, " cm");
  addList("state", p.stateMode, p.states);
  addList("city", p.cityMode, p.cities);
  addList("country_of_residence", p.countryOfResidenceMode, p.countriesOfResidence);
  addList("marital_status", p.maritalStatusMode, p.maritalStatuses);
  addList("education", p.educationMode, p.educationLevels);
  addList("occupation", p.occupationMode, p.occupationCategories);
  addList("diet", p.dietMode, p.diets);
  addList("smoking", p.smokingMode, p.smoking);
  addList("drinking", p.drinkingMode, p.drinking);
  addList("family_values", p.familyValuesMode, p.familyValues);
  addList("relocation", p.relocationMode, p.relocationStatuses);

  return { shared: true, items: items.slice(0, 12) };
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
  ] = await Promise.all([
    db.collection("users").doc(requesterUid).get(),
    db.collection("users").doc(targetUid).get(),
    db.collection("partnerPreferences").doc(targetUid).get(),
    db.collection("blocks").doc(requesterUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(requesterUid).get(),
    db.collection("privacyRelations").doc(requesterUid).collection("members").doc(targetUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(requesterUid).get(),
  ]);

  if (!requester.exists || !target.exists ||
      !accountIsActive(requester.data()?.accountStatus) ||
      !accountIsActive(target.data()?.accountStatus)) {
    throw new functions.https.HttpsError("failed-precondition", "Profile is unavailable");
  }
  if (requesterBlock.exists || targetBlock.exists ||
      requesterRelation.data()?.profileHidden === true ||
      targetRelation.data()?.profileHidden === true) {
    throw new functions.https.HttpsError("permission-denied", "Profile is unavailable");
  }

  return publicSummary(preferences.exists ? preferences.data() : DEFAULT_PARTNER_PREFERENCES);
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
