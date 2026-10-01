import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { accountIsActive } from "./accountStatusPolicy";
import { requireActiveConsent } from "./consent";
import {
  QUESTIONNAIRE_FORMULA_VERSION,
  sanitizeQuestionnaireVectors,
} from "./questionnaireCompatibilityPolicy";
import { db, requireAppCheck } from "./shared";

const QUESTIONNAIRE_SCHEMA_VERSION = 1;

async function requireActiveProfile(uid: string): Promise<void> {
  const profile = await db.collection("users").doc(uid).get();
  if (!profile.exists) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Complete your profile before saving questionnaire answers"
    );
  }
  if (!accountIsActive(profile.data()?.accountStatus)) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Questionnaire updates are unavailable while the account is not active"
    );
  }
}

/**
 * Stores only normalized vectors in an owner-private server document.
 * Peers never receive these vectors; discovery returns only a pair compatibility score.
 */
export const saveQuestionnaire = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  await Promise.all([
    requireActiveProfile(uid),
    requireActiveConsent(uid, "sensitive_preferences"),
  ]);

  const vectors = sanitizeQuestionnaireVectors(
    data?.selfVector,
    data?.partnerVector
  );
  if (!vectors) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Questionnaire vectors are invalid"
    );
  }

  const ref = db.collection("questionnaires").doc(uid);
  const existing = await ref.get();
  await ref.set({
    uid,
    selfVector: vectors.selfVector,
    partnerVector: vectors.partnerVector,
    schemaVersion: QUESTIONNAIRE_SCHEMA_VERSION,
    formulaVersion: QUESTIONNAIRE_FORMULA_VERSION,
    createdAt: existing.data()?.createdAt ||
      admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: false });

  return {
    saved: true,
    schemaVersion: QUESTIONNAIRE_SCHEMA_VERSION,
    formulaVersion: QUESTIONNAIRE_FORMULA_VERSION,
  };
});

/**
 * Owner-only read used to restore questionnaire editing state on another signed-in device.
 * No peer-facing endpoint returns vectors.
 */
export const getMyQuestionnaire = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const snapshot = await db.collection("questionnaires").doc(uid).get();
  if (!snapshot.exists) {
    return {
      completed: false,
      schemaVersion: QUESTIONNAIRE_SCHEMA_VERSION,
      formulaVersion: QUESTIONNAIRE_FORMULA_VERSION,
    };
  }
  const data = snapshot.data() || {};
  const vectors = sanitizeQuestionnaireVectors(
    data.selfVector,
    data.partnerVector
  );
  if (!vectors) {
    functions.logger.error("Stored questionnaire is malformed", { uid });
    throw new functions.https.HttpsError(
      "data-loss",
      "Saved questionnaire data needs to be completed again"
    );
  }

  return {
    completed: true,
    selfVector: vectors.selfVector,
    partnerVector: vectors.partnerVector,
    schemaVersion: QUESTIONNAIRE_SCHEMA_VERSION,
    formulaVersion: QUESTIONNAIRE_FORMULA_VERSION,
    updatedAtMillis: data.updatedAt instanceof admin.firestore.Timestamp
      ? data.updatedAt.toMillis()
      : null,
  };
});
