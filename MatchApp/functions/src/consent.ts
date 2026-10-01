import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { db, requireAppCheck } from "./shared";

export const CONSENT_PURPOSES = [
  "identity_verification",
  "aadhaar_offline_verification",
  "personalization",
  "marketing",
  "location",
  "media_processing",
  "sensitive_preferences",
] as const;

export type ConsentPurpose = typeof CONSENT_PURPOSES[number];

const CONSENT_VERSIONS: Record<ConsentPurpose, string> = {
  identity_verification: "2026-09-29.1",
  aadhaar_offline_verification: "2026-09-29.1",
  personalization: "2026-09-29.1",
  marketing: "2026-09-29.1",
  location: "2026-09-29.1",
  media_processing: "2026-09-29.1",
  sensitive_preferences: "2026-09-29.1",
};

function parsePurpose(value: unknown): ConsentPurpose {
  const purpose = typeof value === "string" ? value.trim() : "";
  if (!CONSENT_PURPOSES.includes(purpose as ConsentPurpose)) {
    throw new functions.https.HttpsError("invalid-argument", "Unsupported consent purpose");
  }
  return purpose as ConsentPurpose;
}

export function currentConsentVersion(purpose: ConsentPurpose): string {
  return CONSENT_VERSIONS[purpose];
}

export function isCurrentConsentRecord(
  purpose: ConsentPurpose,
  value: Record<string, unknown> | undefined
): boolean {
  return value?.granted === true &&
    value?.noticeVersion === currentConsentVersion(purpose);
}

function safeLocale(value: unknown): string {
  const locale = typeof value === "string" ? value.trim() : "";
  return /^[A-Za-z]{2,3}(?:-[A-Za-z]{2})?$/.test(locale) ? locale : "en-IN";
}

export async function hasActiveConsent(uid: string, purpose: ConsentPurpose): Promise<boolean> {
  const snapshot = await db.collection("consents").doc(uid).collection("items").doc(purpose).get();
  const data = snapshot.exists ? snapshot.data() as Record<string, unknown> : undefined;
  return snapshot.exists && isCurrentConsentRecord(purpose, data);
}

export async function requireActiveConsent(uid: string, purpose: ConsentPurpose): Promise<void> {
  if (await hasActiveConsent(uid, purpose)) return;
  throw new functions.https.HttpsError(
    "failed-precondition",
    "Current consent is required before this operation can continue"
  );
}

/**
 * Records the current consent projection and an immutable event in one transaction.
 *
 * The caller cannot choose an arbitrary notice version: the backend publishes the currently
 * accepted version and rejects stale clients. Withdrawal uses the same endpoint with granted=false.
 */
export const recordConsent = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const purpose = parsePurpose(data?.purpose);
  if (typeof data?.granted !== "boolean") {
    throw new functions.https.HttpsError("invalid-argument", "Consent decision is required");
  }
  const granted = data.granted as boolean;
  const noticeVersion = typeof data?.noticeVersion === "string" ? data.noticeVersion.trim() : "";
  const currentVersion = currentConsentVersion(purpose);
  if (noticeVersion !== currentVersion) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "The consent notice has changed. Refresh this screen and review the current notice."
    );
  }

  const currentRef = db.collection("consents").doc(uid).collection("items").doc(purpose);
  const eventRef = db.collection("consentLedger").doc(uid).collection("events").doc();
  const locale = safeLocale(data?.locale);

  await db.runTransaction(async (tx) => {
    const prior = await tx.get(currentRef);
    const priorData = prior.data() || {};
    const changed =
      !prior.exists ||
      priorData.granted !== granted ||
      priorData.noticeVersion !== currentVersion;

    tx.set(currentRef, {
      purpose,
      granted,
      noticeVersion: currentVersion,
      locale,
      source: "ANDROID",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });

    tx.create(eventRef, {
      uid,
      purpose,
      granted,
      noticeVersion: currentVersion,
      locale,
      source: "ANDROID",
      changed,
      recordedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return { success: true, purpose, granted, noticeVersion: currentVersion };
});

export const getConsentState = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const snapshot = await db.collection("consents").doc(uid).collection("items").get();
  const values = new Map(snapshot.docs.map((doc) => [doc.id, doc.data()]));

  return {
    purposes: CONSENT_PURPOSES.map((purpose) => {
      const value = values.get(purpose) || {};
      return {
        purpose,
        granted: value.granted === true,
        noticeVersion: currentConsentVersion(purpose),
        recordedNoticeVersion: typeof value.noticeVersion === "string" ? value.noticeVersion : null,
      };
    }),
  };
});


/**
 * Consent withdrawal is an active control, not just a ledger entry. Revoking location consent
 * immediately removes the server-side exact Nearby point even if the originating device is gone.
 */
export const onLocationConsentChanged = functions.firestore
  .document("consents/{uid}/items/location")
  .onWrite(async (change, context) => {
    const after = change.after.exists
      ? change.after.data() as Record<string, unknown>
      : undefined;
    if (isCurrentConsentRecord("location", after)) return;
    await db.collection("userLocations").doc(context.params.uid).delete();
  });


async function deleteQueryInBatches(query: FirebaseFirestore.Query): Promise<void> {
  let hasMore = true;
  while (hasMore) {
    const snapshot = await query.limit(300).get();
    if (snapshot.empty) return;
    const batch = db.batch();
    snapshot.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    hasMore = snapshot.size === 300;
  }
}

/** Withdrawing personalization removes the signed-in user's optional ranking-feedback history. */
export const onPersonalizationConsentChanged = functions.firestore
  .document("consents/{uid}/items/personalization")
  .onWrite(async (change, context) => {
    const after = change.after.exists ? change.after.data() : undefined;
    if (after?.granted === true &&
        after?.noticeVersion === currentConsentVersion("personalization")) return;
    const uid = context.params.uid;
    await Promise.all([
      deleteQueryInBatches(
        db.collection("recommendationFeedback").doc(uid).collection("targets")
      ),
      deleteQueryInBatches(
        db.collection("recommendationImpressionBatches").where("viewerUid", "==", uid)
      ),
    ]);
    await db.collection("recommendationFeedback").doc(uid).delete();
  });

/** Withdrawing sensitive-preference consent removes durable partner-preference processing input. */
export const onSensitivePreferencesConsentChanged = functions.firestore
  .document("consents/{uid}/items/sensitive_preferences")
  .onWrite(async (change, context) => {
    const after = change.after.exists ? change.after.data() : undefined;
    if (after?.granted === true &&
        after?.noticeVersion === currentConsentVersion("sensitive_preferences")) return;
    const uid = context.params.uid;
    const rmRequestRef = db.collection("rmRequests").doc(uid);
    const rmRequest = await rmRequestRef.get();
    const cleanup: Promise<unknown>[] = [
      db.collection("partnerPreferences").doc(uid).delete(),
    ];
    if (rmRequest.exists && String(rmRequest.data()?.preferences || "").length > 0) {
      cleanup.push(
        rmRequestRef.set({
          preferences: "",
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true })
      );
    }
    await Promise.all(cleanup);
  });


/**
 * Withdrawal cancels an in-flight generic identity review and deletes its raw object. Completed
 * verification outcomes are not silently rewritten here; their minimum-retention policy is
 * handled separately from optional raw-document processing.
 */
export const onIdentityVerificationConsentChanged = functions.firestore
  .document("consents/{uid}/items/identity_verification")
  .onWrite(async (change, context) => {
    const after = change.after.exists ? change.after.data() as Record<string, unknown> : undefined;
    if (isCurrentConsentRecord("identity_verification", after)) return;

    const uid = context.params.uid;
    const requestRef = db.collection("verificationRequests").doc(uid);
    const request = await requestRef.get();
    if (!request.exists || String(request.data()?.status || "").toLowerCase() !== "pending") return;

    const documentPath = String(request.data()?.documentPath || "");
    if (documentPath.startsWith(`verifications/${uid}/`) && !documentPath.includes("..")) {
      await admin.storage().bucket().file(documentPath).delete({ ignoreNotFound: true });
    }
    await requestRef.set({
      status: "withdrawn",
      documentPath: admin.firestore.FieldValue.delete(),
      documentDeletedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  });

/**
 * Withdrawal cancels only pending profile-photo processing. Already approved profile photos remain
 * user-controlled profile data and can be removed through the normal delete-photo flow.
 */
export const onMediaProcessingConsentChanged = functions.firestore
  .document("consents/{uid}/items/media_processing")
  .onWrite(async (change, context) => {
    const after = change.after.exists ? change.after.data() as Record<string, unknown> : undefined;
    if (isCurrentConsentRecord("media_processing", after)) return;

    const uid = context.params.uid;
    const [pendingPhotos, pendingVideos] = await Promise.all([
      db.collection("photoModeration")
        .where("uid", "==", uid)
        .where("status", "==", "PENDING")
        .limit(100)
        .get(),
      db.collection("videoModeration")
        .where("uid", "==", uid)
        .where("status", "==", "PENDING")
        .limit(100)
        .get(),
    ]);

    for (const doc of pendingPhotos.docs) {
      const storagePath = String(doc.data()?.storagePath || "");
      if (storagePath.startsWith(`photos/${uid}/`) && !storagePath.includes("..")) {
        await admin.storage().bucket().file(storagePath).delete({ ignoreNotFound: true });
      }
      await doc.ref.delete();
    }
    for (const doc of pendingVideos.docs) {
      const storagePath = String(doc.data()?.storagePath || "");
      if (storagePath.startsWith(`videos/${uid}/`) && !storagePath.includes("..")) {
        await admin.storage().bucket().file(storagePath).delete({ ignoreNotFound: true });
      }
      await doc.ref.delete();
    }
  });
