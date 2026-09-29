import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { db, requireAppCheck } from "./shared";
import { DEFAULT_PARTNER_PREFERENCES } from "./partnerPreferencesPolicy";

const RELIGIONS: Record<string, { id: string; label: string }> = {
  "hindu": { id: "HINDU", label: "Hindu" },
  "muslim": { id: "MUSLIM", label: "Muslim" },
  "christian": { id: "CHRISTIAN", label: "Christian" },
  "sikh": { id: "SIKH", label: "Sikh" },
  "buddhist": { id: "BUDDHIST", label: "Buddhist" },
  "jain": { id: "JAIN", label: "Jain" },
  "parsi / zoroastrian": { id: "PARSI_ZOROASTRIAN", label: "Parsi / Zoroastrian" },
  "other": { id: "OTHER", label: "Other" },
  "prefer not to say": { id: "PREFER_NOT_TO_SAY", label: "Prefer not to say" },
};

function parseReligion(value: unknown): { id: string; label: string } | null {
  if (typeof value !== "string") return null;
  return RELIGIONS[value.trim().toLowerCase()] ?? null;
}

/**
 * Confirms canonical matrimonial-profile religion exactly once.
 * Corrections after lock must use a separately audited support/admin workflow.
 */
export const confirmReligion = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Sign in before confirming religion");
  }

  const religion = parseReligion(data?.religion);
  if (!religion) {
    throw new functions.https.HttpsError("invalid-argument", "Choose a supported religion value");
  }

  const profileRef = db.collection("users").doc(uid);
  const privateRef = db.collection("userPrivate").doc(uid);
  const preferenceRef = db.collection("partnerPreferences").doc(uid);

  await db.runTransaction(async (tx) => {
    const [profile, existingPreferences] = await Promise.all([
      tx.get(profileRef),
      tx.get(preferenceRef),
    ]);
    if (!profile.exists) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Create your profile before confirming religion"
      );
    }

    const current = profile.data() ?? {};
    if (current.religionLocked === true) {
      const currentId = typeof current.religionId === "string" ? current.religionId : null;
      if (currentId !== religion.id) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Religion is already confirmed. Use the protected correction process if it is incorrect."
        );
      }
    } else {
      tx.set(profileRef, {
        religion: religion.label,
        religionId: religion.id,
        religionLocked: true,
        religionConfirmedAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }

    // First-run discovery follows the explicitly confirmed religion without coupling visual theme
    // to match eligibility. The preference remains fully editable later; an existing preference
    // document is never overwritten.
    const bootstrapSameReligion =
      religion.id !== "OTHER" && religion.id !== "PREFER_NOT_TO_SAY";
    if (!existingPreferences.exists && bootstrapSameReligion) {
      tx.set(preferenceRef, {
        ...DEFAULT_PARTNER_PREFERENCES,
        configured: true,
        religionMode: "STRICT",
        religions: [religion.label],
        schemaVersion: 2,
        source: "RELIGION_CONFIRMATION_BOOTSTRAP",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: false });
    }

    if (religion.id !== "HINDU") {
      tx.set(profileRef, {
        gothra: admin.firestore.FieldValue.delete(),
      }, { merge: true });
      tx.set(privateRef, {
        rasi: admin.firestore.FieldValue.delete(),
        nakshatra: admin.firestore.FieldValue.delete(),
        manglik: admin.firestore.FieldValue.delete(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }
  });

  return { religionId: religion.id, religion: religion.label, locked: true };
});
