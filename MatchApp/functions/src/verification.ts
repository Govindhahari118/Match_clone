import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { createHash } from "crypto";
import {
  currentConsentVersion,
  requireActiveConsent,
} from "./consent";
import { db, persistAndSendNotification, requireAppCheck, requireOpsRole } from "./shared";

const GENERIC_VERIFICATION_TYPES = [
  "Passport",
  "Driving Licence",
  "PAN Card",
  "Voter ID",
] as const;
const VERIFICATION_TYPES = new Set<string>(GENERIC_VERIFICATION_TYPES);
const MAX_VERIFICATION_BYTES = 5 * 1024 * 1024;

// Aadhaar is deliberately not accepted by the generic document-upload path. A production Aadhaar
// option must use an explicitly implemented, registered OVSE/provider flow with signature validation
// and its own consent contract. This remains false until that provider adapter exists in source.
const AADHAAR_OFFLINE_PROVIDER_IMPLEMENTED = false;

function requireDocumentPath(uid: string, value: unknown): string {
  const path = typeof value === "string" ? value.trim() : "";
  const prefix = `verifications/${uid}/`;
  if (!path.startsWith(prefix) || path.length <= prefix.length || path.length > 512 || path.includes("..")) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid verification document path");
  }
  return path;
}

async function deleteReviewedDocument(uid: string, documentPath: unknown): Promise<boolean> {
  if (typeof documentPath !== "string" || !documentPath.startsWith(`verifications/${uid}/`)) {
    return false;
  }
  try {
    await admin.storage().bucket().file(documentPath).delete();
  } catch (err) {
    const code = (err as { code?: number | string })?.code;
    if (code !== 404 && code !== "404") {
      functions.logger.error("Reviewed verification document deletion failed", {
        uid,
        documentPath,
        error: err instanceof Error ? err.message : String(err),
      });
      return false;
    }
  }

  await db.collection("verificationRequests").doc(uid).set({
    documentDeletedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  return true;
}

export const getVerificationOptions = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  if (!context.auth?.uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  return {
    genericDocumentTypes: GENERIC_VERIFICATION_TYPES,
    identityConsentVersion: currentConsentVersion("identity_verification"),
    aadhaarOfflineAvailable: AADHAAR_OFFLINE_PROVIDER_IMPLEMENTED,
    aadhaarConsentVersion: currentConsentVersion("aadhaar_offline_verification"),
    aadhaarUnavailableReason: AADHAAR_OFFLINE_PROVIDER_IMPLEMENTED
      ? null
      : "registered_ovse_provider_flow_not_integrated",
  };
});

/**
 * A client may upload its own protected KYC object, but only trusted backend code can convert that
 * object into a PENDING verification request. The callable verifies current consent, object
 * existence, owner metadata, declared document type, content type and size first so the UI cannot
 * manufacture a pending/verified state.
 *
 * Aadhaar is intentionally excluded here; it must never be treated as a generic card-image upload.
 */
export const confirmPhoneVerification = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const authUser = await admin.auth().getUser(uid);
  const phoneNumber = authUser.phoneNumber?.trim() || "";
  if (!phoneNumber) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "A phone credential must be linked to this signed-in account first"
    );
  }

  const batch = db.batch();
  batch.set(db.collection("verifications").doc(uid), {
    phoneStatus: "VERIFIED",
    phoneVerifiedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  batch.set(db.collection("userPrivate").doc(uid), {
    phoneNumber,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  await batch.commit();

  return { success: true, phoneStatus: "VERIFIED", phoneNumber };
});

export const submitVerificationRequest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  await requireActiveConsent(uid, "identity_verification");

  const docType = typeof data?.docType === "string" ? data.docType.trim() : "";
  if (docType.toLowerCase().includes("aadhaar")) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Aadhaar cannot be uploaded through the generic government-ID review flow"
    );
  }
  if (!VERIFICATION_TYPES.has(docType)) {
    throw new functions.https.HttpsError("invalid-argument", "Choose a supported government ID type");
  }
  const documentPath = requireDocumentPath(uid, data?.documentPath);

  const file = admin.storage().bucket().file(documentPath);
  let metadata: { size?: string | number; contentType?: string; metadata?: Record<string, string> };
  try {
    const [raw] = await file.getMetadata();
    metadata = raw as typeof metadata;
  } catch (err) {
    functions.logger.warn("Verification object missing", {
      uid,
      documentPath,
      error: err instanceof Error ? err.message : String(err),
    });
    throw new functions.https.HttpsError("failed-precondition", "Upload the verification document before submitting");
  }

  const size = Number(metadata.size || 0);
  const contentType = String(metadata.contentType || "").toLowerCase();
  const ownerUid = metadata.metadata?.ownerUid || "";
  const uploadedDocType = metadata.metadata?.docType || "";
  if (!Number.isFinite(size) || size <= 0 || size > MAX_VERIFICATION_BYTES) {
    throw new functions.https.HttpsError("invalid-argument", "Verification document must be 5 MB or smaller");
  }
  if (!(contentType.startsWith("image/") || contentType === "application/pdf")) {
    throw new functions.https.HttpsError("invalid-argument", "Verification document must be an image or PDF");
  }
  if (ownerUid !== uid) {
    throw new functions.https.HttpsError("permission-denied", "Verification document ownership mismatch");
  }
  if (uploadedDocType !== docType) {
    throw new functions.https.HttpsError("permission-denied", "Verification document type metadata mismatch");
  }

  const requestRef = db.collection("verificationRequests").doc(uid);
  const existing = await requestRef.get();
  const existingStatus = String(existing.data()?.status || "").toLowerCase();
  if (existingStatus === "verified") {
    return { success: true, status: "VERIFIED" };
  }
  if (existingStatus === "pending" && existing.data()?.documentPath === documentPath) {
    return { success: true, status: "PENDING" };
  }

  await requestRef.set({
    uid,
    docType,
    verificationMethod: "GENERIC_GOVERNMENT_ID_REVIEW",
    documentPath,
    documentDeletedAt: admin.firestore.FieldValue.delete(),
    status: "pending",
    rejectionReason: "",
    submittedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    reviewedAt: admin.firestore.FieldValue.delete(),
    reviewedBy: admin.firestore.FieldValue.delete(),
  }, { merge: true });

  functions.logger.info("Verification request submitted", { uid, docType });
  return { success: true, status: "PENDING" };
});

export const onVerificationSubmitted = functions.firestore
  .document("verificationRequests/{uid}")
  .onCreate(async (snap, context) => {
    const uid = context.params.uid;
    const data = snap.data();
    functions.logger.info("Verification request created", {
      uid,
      docType: typeof data.docType === "string" ? data.docType : "unknown",
      method: typeof data.verificationMethod === "string" ? data.verificationMethod : "unknown",
    });
  });

export const approveVerification = functions.https.onCall(async (data, context) => {
  const reviewer = requireOpsRole(context, ["kyc_reviewer", "ops_admin"]);
  const reviewerUid = reviewer.uid;
  const targetUid = typeof data?.targetUid === "string" ? data.targetUid.trim() : "";
  const approved = data?.approved === true;
  const rejectionReason = typeof data?.rejectionReason === "string" ? data.rejectionReason.trim().slice(0, 500) : "";
  const newLevel = Number(data?.newLevel);

  if (!targetUid) throw new functions.https.HttpsError("invalid-argument", "targetUid required");
  if (approved && (!Number.isInteger(newLevel) || newLevel < 2 || newLevel > 5)) {
    throw new functions.https.HttpsError("invalid-argument", "Government-ID approval level must be an integer from 2 to 5");
  }
  if (!approved && rejectionReason.length < 3) {
    throw new functions.https.HttpsError("invalid-argument", "A rejection reason is required");
  }

  const requestRef = db.collection("verificationRequests").doc(targetUid);
  const profileRef = db.collection("users").doc(targetUid);
  const [requestSnap, profileSnap] = await Promise.all([requestRef.get(), profileRef.get()]);
  if (!requestSnap.exists) throw new functions.https.HttpsError("not-found", "Verification request not found");
  if (!profileSnap.exists) throw new functions.https.HttpsError("not-found", "Target profile not found");
  if (String(requestSnap.data()?.status || "").toLowerCase() !== "pending") {
    throw new functions.https.HttpsError("failed-precondition", "Only pending verification requests can be reviewed");
  }
  if (String(requestSnap.data()?.verificationMethod || "") !== "GENERIC_GOVERNMENT_ID_REVIEW") {
    throw new functions.https.HttpsError("failed-precondition", "Unsupported verification review method");
  }

  const previousLevel = Number(profileSnap.data()?.verificationLevel || 0);
  const previousVerified = profileSnap.data()?.isVerified === true;
  const nextStatus = approved ? "verified" : "rejected";
  const nextLevel = approved ? newLevel : previousLevel;
  const nextVerified = approved ? newLevel >= 2 : previousVerified;
  const effectiveReason = approved ? "APPROVED" : rejectionReason;

  const batch = db.batch();
  batch.update(requestRef, {
    status: nextStatus,
    rejectionReason: approved ? "" : rejectionReason,
    reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
    reviewedBy: reviewerUid,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  if (approved) {
    batch.update(profileRef, {
      verificationLevel: newLevel,
      isVerified: newLevel >= 2,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }
  batch.set(db.collection("adminAudit").doc(), {
    actorUid: reviewerUid,
    actorRole: reviewer.role,
    action: "verification.review",
    targetUid,
    before: {
      requestStatus: "pending",
      verificationLevel: previousLevel,
      isVerified: previousVerified,
    },
    after: {
      requestStatus: nextStatus,
      verificationLevel: nextLevel,
      isVerified: nextVerified,
    },
    reason: effectiveReason,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  await batch.commit();

  const requestKey = createHash("sha256")
    .update(String(requestSnap.data()?.documentPath || targetUid))
    .digest("hex")
    .slice(0, 24);

  const rawDocumentDeleted = await deleteReviewedDocument(
    targetUid,
    requestSnap.data()?.documentPath
  );

  try {
    await persistAndSendNotification({
      notificationId: `verification_${targetUid}_${requestKey}_${approved ? "approved" : "rejected"}`,
      userId: targetUid,
      type: "VERIFICATION",
      title: approved ? "Verification approved" : "Verification update",
      body: approved ?
        "Your profile verification was approved." :
        "Your verification needs attention. Open Matree for details.",
      entityType: "verification",
      entityId: targetUid,
      deepLink: "matrimonyconnect://verification",
      pushType: "verification_update",
      preferenceKey: "system",
      priority: "high",
    });
  } catch (err) {
    functions.logger.warn("Verification notification delivery failed", {
      targetUid,
      error: err instanceof Error ? err.message : String(err),
    });
  }
  return { success: true, rawDocumentDeleted };
});

/**
 * Defense-in-depth cleanup for previously reviewed KYC objects when a transient Storage failure
 * prevented deletion during the review request itself.
 */
export const cleanupReviewedVerificationDocuments = functions.pubsub
  .schedule("every 24 hours")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const reviewed = await db.collection("verificationRequests")
      .where("status", "in", ["verified", "rejected"])
      .limit(200)
      .get();

    for (const doc of reviewed.docs) {
      if (doc.data()?.documentDeletedAt) continue;
      await deleteReviewedDocument(doc.id, doc.data()?.documentPath);
    }
  });
