import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { db, getFcmToken, messaging, requireAppCheck } from "./shared";

function requireAdmin(context: functions.https.CallableContext): string {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  if (context.auth?.token.admin !== true) {
    throw new functions.https.HttpsError("permission-denied", "Admin access required");
  }
  return uid;
}

export const onVerificationSubmitted = functions.firestore
  .document("verificationRequests/{uid}")
  .onCreate(async (snap, context) => {
    const uid = context.params.uid;
    const data = snap.data();
    functions.logger.info("Verification request submitted", {
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

  const batch = db.batch();
  batch.update(requestRef, {
    status: approved ? "verified" : "rejected",
    rejectionReason: approved ? "" : (rejectionReason || "Please re-submit"),
    reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
    reviewedBy: reviewerUid,
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
