import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

const FREE_INTERESTS_PER_UTC_DAY = 5;
const MAX_UID_LENGTH = 128;

function requireTargetUid(value: unknown, ownUid: string): string {
  const targetUid = typeof value === "string" ? value.trim() : "";
  if (!targetUid || targetUid === ownUid || targetUid.length > MAX_UID_LENGTH) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target profile");
  }
  return targetUid;
}

function utcDayKey(now = new Date()): string {
  return now.toISOString().slice(0, 10);
}

function activePremium(user: FirebaseFirestore.DocumentData, nowMillis: number): boolean {
  if (user.isPremium !== true) return false;
  const premiumUntil = user.premiumUntil instanceof admin.firestore.Timestamp
    ? user.premiumUntil.toMillis()
    : Number(user.subscriptionExpiry || 0);
  return Number.isFinite(premiumUntil) && premiumUntil > nowMillis;
}

/**
 * Server-authoritative interest creation.
 *
 * The caller cannot choose the sender identity, create a match directly, or bypass the free-tier
 * daily quota with a modified APK. A reverse pending interest atomically creates the mutual match.
 */
export const sendInterest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const fromUid = context.auth?.uid;
  if (!fromUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const toUid = requireTargetUid(data?.toUid, fromUid);
  const isSuperLike = data?.isSuperLike === true;
  const interestRef = db.collection("interests").doc(`${fromUid}_${toUid}`);
  const reverseRef = db.collection("interests").doc(`${toUid}_${fromUid}`);
  const matchRef = db.collection("matches").doc([fromUid, toUid].sort().join("_"));
  const fromUserRef = db.collection("users").doc(fromUid);
  const toUserRef = db.collection("users").doc(toUid);
  const forwardPrivacyRef = db.collection("privacyRelations").doc(fromUid).collection("members").doc(toUid);
  const reversePrivacyRef = db.collection("privacyRelations").doc(toUid).collection("members").doc(fromUid);
  const forwardBlockRef = db.collection("blocks").doc(fromUid).collection("blocked").doc(toUid);
  const reverseBlockRef = db.collection("blocks").doc(toUid).collection("blocked").doc(fromUid);
  const dayKey = utcDayKey();
  // Reuse the existing server-only usage subtree so account deletion already removes quota data.
  const usageRef = db.collection("subscriptions").doc(fromUid).collection("usage").doc(`interests_${dayKey}`);
  const nowMillis = Date.now();

  const result = await db.runTransaction(async (tx) => {
    const [
      existingInterest,
      reverseInterest,
      existingMatch,
      fromUser,
      toUser,
      forwardPrivacy,
      reversePrivacy,
      forwardBlock,
      reverseBlock,
      usage,
    ] = await Promise.all([
      tx.get(interestRef),
      tx.get(reverseRef),
      tx.get(matchRef),
      tx.get(fromUserRef),
      tx.get(toUserRef),
      tx.get(forwardPrivacyRef),
      tx.get(reversePrivacyRef),
      tx.get(forwardBlockRef),
      tx.get(reverseBlockRef),
      tx.get(usageRef),
    ]);

    if (!fromUser.exists || !toUser.exists) {
      throw new functions.https.HttpsError("not-found", "Profile not found");
    }
    if (forwardBlock.exists || reverseBlock.exists) {
      throw new functions.https.HttpsError("permission-denied", "Interest is unavailable for this relationship");
    }
    if (forwardPrivacy.data()?.profileHidden === true || reversePrivacy.data()?.profileHidden === true) {
      throw new functions.https.HttpsError("permission-denied", "Interest is unavailable for this privacy relationship");
    }

    // A stealth target cannot be addressed by guessing its UID. If that target already sent the
    // caller an interest, the explicit request makes accepting it a legitimate mutual action.
    if (toUser.data()?.stealthMode === true && !reverseInterest.exists) {
      throw new functions.https.HttpsError("permission-denied", "Profile is not currently discoverable");
    }

    if (existingInterest.exists) {
      return {
        mutual: existingMatch.exists || reverseInterest.exists,
        remaining: activePremium(fromUser.data() || {}, nowMillis)
          ? null
          : Math.max(0, FREE_INTERESTS_PER_UTC_DAY - Number(usage.data()?.count || 0)),
        alreadySent: true,
      };
    }

    const premium = activePremium(fromUser.data() || {}, nowMillis);
    const used = Number(usage.data()?.count || 0);
    if (!premium && used >= FREE_INTERESTS_PER_UTC_DAY) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        `Free members can send ${FREE_INTERESTS_PER_UTC_DAY} interests per UTC day`
      );
    }

    tx.set(interestRef, {
      fromUid,
      toUid,
      isSuperLike,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });

    if (!premium) {
      tx.set(usageRef, {
        uid: fromUid,
        dayKey,
        count: used + 1,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: false });
    }

    const mutual = reverseInterest.exists || existingMatch.exists;
    if (reverseInterest.exists && !existingMatch.exists) {
      tx.set(matchRef, {
        users: [fromUid, toUid].sort(),
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        lastActivity: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: false });
    }

    return {
      mutual,
      remaining: premium ? null : Math.max(0, FREE_INTERESTS_PER_UTC_DAY - used - 1),
      alreadySent: false,
    };
  });

  return { success: true, ...result };
});

/**
 * Withdraw only a still-pending outgoing interest. Mutual relationships are not silently broken by
 * toggling a local button; they require an explicit future unmatch/block policy.
 */
export const withdrawInterest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const fromUid = context.auth?.uid;
  if (!fromUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const toUid = requireTargetUid(data?.toUid, fromUid);
  const outgoingRef = db.collection("interests").doc(`${fromUid}_${toUid}`);
  const reverseRef = db.collection("interests").doc(`${toUid}_${fromUid}`);
  const matchRef = db.collection("matches").doc([fromUid, toUid].sort().join("_"));

  await db.runTransaction(async (tx) => {
    const [outgoing, reverse, match] = await Promise.all([
      tx.get(outgoingRef),
      tx.get(reverseRef),
      tx.get(matchRef),
    ]);
    if (!outgoing.exists) return;
    if (reverse.exists || match.exists) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "A mutual match cannot be withdrawn as a pending interest"
      );
    }
    if (outgoing.data()?.fromUid !== fromUid || outgoing.data()?.toUid !== toUid) {
      throw new functions.https.HttpsError("permission-denied", "Interest does not belong to you");
    }
    tx.delete(outgoingRef);
  });

  return { success: true };
});

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
  if (!senderUid || senderUid === recipientUid || senderUid.length > MAX_UID_LENGTH) {
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
