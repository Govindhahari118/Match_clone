import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

/**
 * Decline a pending incoming interest without blocking the sender.
 * Only the authenticated recipient may decline, and an already-mutual relationship must use
 * a separate unmatch flow rather than silently deleting one side of a match.
 */
export const declineInterest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const recipientUid = context.auth?.uid;
  if (!recipientUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const senderUid = typeof data?.senderUid === "string" ? data.senderUid.trim() : "";
  if (!senderUid || senderUid === recipientUid || senderUid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid sender profile");
  }

  const incomingRef = db.collection("interests").doc(`${senderUid}_${recipientUid}`);
  const reverseRef = db.collection("interests").doc(`${recipientUid}_${senderUid}`);
  const matchRef = db.collection("matches").doc([senderUid, recipientUid].sort().join("_"));

  await db.runTransaction(async (tx) => {
    const [incoming, reverse, match] = await Promise.all([
      tx.get(incomingRef),
      tx.get(reverseRef),
      tx.get(matchRef),
    ]);

    if (!incoming.exists) return;
    if (incoming.data()?.toUid !== recipientUid || incoming.data()?.fromUid !== senderUid) {
      throw new functions.https.HttpsError("permission-denied", "This request does not belong to you");
    }
    if (reverse.exists || match.exists) {
      throw new functions.https.HttpsError("failed-precondition", "This request is already a mutual match");
    }

    tx.delete(incomingRef);
    tx.set(db.collection("interestResponses").doc(`${senderUid}_${recipientUid}`), {
      senderUid,
      recipientUid,
      status: "declined",
      respondedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });
  });

  return { success: true };
});
