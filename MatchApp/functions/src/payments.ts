import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { db } from "./shared";

// ── Razorpay Payment Verification ──────────────────────────────────────────
export const verifyRazorpayPayment = functions.https.onCall(
  async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Must be authenticated");

    const { orderId, paymentId, signature, planType } = data;
    const crypto = await import("crypto");

    const razorpayKeySecret = functions.config().razorpay?.key_secret;
    if (!razorpayKeySecret) throw new functions.https.HttpsError("failed-precondition", "Razorpay secret not configured");

    const generatedSignature = crypto
      .createHmac("sha256", razorpayKeySecret)
      .update(`${orderId}|${paymentId}`)
      .digest("hex");

    if (generatedSignature !== signature) throw new functions.https.HttpsError("invalid-argument", "Payment verification failed");

    const uid = context.auth.uid;
    const now = admin.firestore.Timestamp.now();

    let premiumUntil: admin.firestore.Timestamp;
    switch (planType) {
      case "silver_3m": premiumUntil = admin.firestore.Timestamp.fromMillis(now.toMillis() + 90 * 24 * 60 * 60 * 1000); break;
      case "gold_6m": premiumUntil = admin.firestore.Timestamp.fromMillis(now.toMillis() + 180 * 24 * 60 * 60 * 1000); break;
      case "platinum_12m": premiumUntil = admin.firestore.Timestamp.fromMillis(now.toMillis() + 365 * 24 * 60 * 60 * 1000); break;
      case "till_you_marry": premiumUntil = admin.firestore.Timestamp.fromMillis(now.toMillis() + 3650 * 24 * 60 * 60 * 1000); break;
      default: throw new functions.https.HttpsError("invalid-argument", "Invalid plan type");
    }

    await db.collection("users").doc(uid).update({
      isPremium: true,
      premiumPlan: planType,
      premiumUntil: premiumUntil,
      paymentId: paymentId,
      updatedAt: now,
    });

    await db.collection("payments").add({
      userId: uid,
      orderId,
      paymentId,
      planType,
      verifiedAt: now,
      amount: data.amount || 0,
    });

    return { success: true, premiumUntil: premiumUntil.toMillis() };
  }
);
