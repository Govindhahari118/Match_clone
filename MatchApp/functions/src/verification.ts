import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { db, getFcmToken, messaging, requireAppCheck } from "./shared";

const VERIFICATION_TYPES = new Set(["Aadhaar", "Passport", "PAN Card", "Voter ID"]);
const MAX_VERIFICATION_BYTES = 5 * 1024 * 1024;

function requireAdmin(context: functions.https.CallableContext): string {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  if (context.auth?.token.admin !== true) {
    throw new functions.https.HttpsError("permission-denied", "Admin access required");
  }
  return uid;
}

function requireDocumentPath(uid: string, value: unknown): string {
  const path = typeof value === "string" ? value.trim() : "";
  const prefix = `verifications/${uid}/`;
  if (!path.startsWith(prefix) || path.length <= prefix.length || path.length > 512 || path.includes("..")) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid verification document path");
  }
  return path;
}

/**
 * A client may upload its own protected KYC object, but only trusted backend code can convert that
 * object into a PENDING verification request. The callable verifies object existence, owner
 * metadata, type and size first so the UI cannot manufacture a pending/verified state.
 */
export const submitVerificationRequest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const docType = typeof data?.docType === "string" ? data.docType.trim() : "";
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
  if (!Number.isFinite(size) || size <= 0 || size > MAX_VERIFICATION_BYTES) {
    throw new functions.https.HttpsError("invalid-argument", "Verification document must be 5 MB or smaller");
  }
  if (!(contentType.startsWith("image/") || contentType === "application/pdf")) {
    throw new functions.https.HttpsError("invalid-argument", "Verification document must be an image or PDF");
  }
  if (ownerUid !== uid) {
    throw new functions.https.HttpsError("permission-denied", "Verification document ownership mismatch");
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
    documentPath,
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
    });
  });

export const approveVerification = functions.https.onCall(async (data, context) => {
  const reviewerUid = requireAdmin(context);
  const targetUid = typeof data?.targetUid === "string" ? data.targetUid.trim() : "";
  const approved = data?.approved === true;
  const rejectionReason = typeof data?.rejectionReason === "string" ? data.rejectionReason.trim().slice(0, 500) : "";
  const newLevel = Number(data?.newLevel);

  if (!targetUid) throw new functions.https.HttpsError("invalid-argument", "targetUid required");
  if (approved && (!Number.isInteger(newLevel) || newLevel < 1 || newLevel > 5)) {
    throw new functions.https.HttpsError("invalid-argument", "newLevel must be an integer from 1 to 5 for approvals");
  }

  const requestRef = db.collection("verificationRequests").doc(targetUid);
  const requestSnap = await requestRef.get();
  if (!requestSnap.exists) throw new functions.https.HttpsError("not-found", "Verification request not found");
  if (String(requestSnap.data()?.status || "").toLowerCase() !== "pending") {
    throw new functions.https.HttpsError("failed-precondition", "Only pending verification requests can be reviewed");
  }

  const batch = db.batch();
  batch.update(requestRef, {
    status: approved ? "verified" : "rejected",
    rejectionReason: approved ? "" : (rejectionReason || "Please re-submit"),
    reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
    reviewedBy: reviewerUid,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  if (approved) {
    batch.update(db.collection("users").doc(targetUid), {
      verificationLevel: newLevel,
      isVerified: newLevel >= 2,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }
  await batch.commit();

  const fcmToken = await getFcmToken(targetUid);
  if (fcmToken) {
    try {
      await messaging.send({
        token: fcmToken,
        data: {
          type: "verification_update",
          title: approved ? "Verification Approved ✅" : "Verification Update",
          body: approved ? "Your profile is now verified." : "Your verification needs attention. Open the app for details.",
          recipient_uid: targetUid,
        },
        android: { priority: "high", notification: { channelId: "match_system" } },
      });
    } catch (err) {
      functions.logger.warn("Verification notification delivery failed", {
        targetUid,
        error: err instanceof Error ? err.message : String(err),
      });
    }
  }
  return { success: true };
});
