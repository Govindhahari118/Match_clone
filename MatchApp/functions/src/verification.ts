import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { db, messaging } from "./shared";

// ── Verification Request Submitted → Admin notification ────────────────────
export const onVerificationSubmitted = functions.firestore
  .document("verificationRequests/{uid}")
  .onCreate(async (snap, context) => {
    const uid = context.params.uid;
    const data = snap.data();
    functions.logger.info(`Verification request from ${uid}: ${data.docType}`);
  });

// ── Approve/Reject Verification (Admin callable) ───────────────────────────
export const approveVerification = functions.https.onCall(
  async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

    const { targetUid, approved, rejectionReason, newLevel } = data;
    if (!targetUid) throw new functions.https.HttpsError("invalid-argument", "targetUid required");

    await db.collection("verificationRequests").doc(targetUid).update({
      status: approved ? "verified" : "rejected",
      rejectionReason: rejectionReason || "",
      reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
      reviewedBy: context.auth.uid,
    });

    if (approved && newLevel) {
      await db.collection("users").doc(targetUid).update({
        verificationLevel: newLevel,
        isVerified: newLevel >= 2,
      });
    }

    const userDoc = await db.collection("users").doc(targetUid).get();
    const fcmToken = userDoc.data()?.fcmToken as string | undefined;
    if (fcmToken) {
      await messaging.send({
        token: fcmToken,
        data: {
          type: "verification_update",
          title: approved ? "Verification Approved ✅" : "Verification Update",
          body: approved
            ? "Your profile is now verified! You'll get a trust badge."
            : `Verification needs attention: ${rejectionReason || "Please re-submit"}`,
        },
        android: { priority: "high", notification: { channelId: "match_system" } },
      });
    }

    return { success: true };
  }
);
