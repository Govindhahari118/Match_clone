import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";
import { declinedInterestAllowsNewRequest } from "./interestPolicy";
import { accountIsActive } from "./accountStatusPolicy";
import {
  discoveryActorReady,
  discoveryCandidateReady,
} from "./discoveryEligibilityPolicy";
import {
  HIGH_VOLUME_INTEREST_SIGNAL_THRESHOLD,
  MAX_DAILY_INTERESTS_SAFETY,
  crossesThreshold,
  safeUsageCount,
  usageAllowed,
} from "./abusePolicy";

const FREE_DAILY_INTEREST_LIMIT = 5;

function requireUid(value: unknown, field: string): string {
  const uid = typeof value === "string" ? value.trim() : "";
  if (!uid || uid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", `Invalid ${field}`);
  }
  return uid;
}

function matchId(uidA: string, uidB: string): string {
  return [uidA, uidB].sort().join("_");
}

function activePaidMembership(user: FirebaseFirestore.DocumentData): boolean {
  const expiry = user.premiumUntil instanceof admin.firestore.Timestamp
    ? user.premiumUntil.toMillis()
    : Number(user.subscriptionExpiry || 0);
  return user.isPremium === true && Number.isFinite(expiry) && expiry > Date.now();
}

function genderCompatible(
  sender: FirebaseFirestore.DocumentData,
  target: FirebaseFirestore.DocumentData
): boolean {
  const accepts = (lookingFor: unknown, gender: unknown): boolean => {
    const looking = String(lookingFor || "ANY").toUpperCase();
    const value = String(gender || "OTHER").toUpperCase();
    if (looking === "ANY") return true;
    return looking === value;
  };
  return accepts(sender.lookingFor, target.gender) && accepts(target.lookingFor, sender.gender);
}

export const sendInterest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const senderUid = context.auth?.uid;
  if (!senderUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = requireUid(data?.targetUid, "target profile");
  if (targetUid === senderUid) {
    throw new functions.https.HttpsError("invalid-argument", "You cannot send an interest to yourself");
  }
  const isSuperLike = data?.isSuperLike === true;

  const senderRef = db.collection("users").doc(senderUid);
  const targetRef = db.collection("users").doc(targetUid);
  const senderPrivateRef = db.collection("userPrivate").doc(senderUid);
  const senderPreferencesRef = db.collection("partnerPreferences").doc(senderUid);
  const targetPreferencesRef = db.collection("partnerPreferences").doc(targetUid);
  const outgoingRef = db.collection("interests").doc(`${senderUid}_${targetUid}`);
  const reverseRef = db.collection("interests").doc(`${targetUid}_${senderUid}`);
  const matchRef = db.collection("matches").doc(matchId(senderUid, targetUid));
  const responseRef = db.collection("interestResponses").doc(`${senderUid}_${targetUid}`);
  const usageDay = new Date().toISOString().slice(0, 10);
  const usageRef = db.collection("subscriptions").doc(senderUid).collection("usage").doc(`interests_${usageDay}`);
  const riskActivityRef = db.collection("riskActivity").doc(senderUid).collection("days").doc(usageDay);
  const riskSignalRef = db.collection("riskSignals").doc(senderUid);
  const senderBlockRef = db.collection("blocks").doc(senderUid).collection("blocked").doc(targetUid);
  const targetBlockRef = db.collection("blocks").doc(targetUid).collection("blocked").doc(senderUid);
  const senderPrivacyRef = db.collection("privacyRelations").doc(senderUid).collection("members").doc(targetUid);
  const targetPrivacyRef = db.collection("privacyRelations").doc(targetUid).collection("members").doc(senderUid);

  return db.runTransaction(async (tx) => {
    const [
      senderSnap,
      targetSnap,
      senderPrivateSnap,
      senderPreferencesSnap,
      targetPreferencesSnap,
      outgoingSnap,
      reverseSnap,
      responseSnap,
      usageSnap,
      senderBlock,
      targetBlock,
      senderPrivacy,
      targetPrivacy,
      riskActivity,
    ] = await Promise.all([
      tx.get(senderRef),
      tx.get(targetRef),
      tx.get(senderPrivateRef),
      tx.get(senderPreferencesRef),
      tx.get(targetPreferencesRef),
      tx.get(outgoingRef),
      tx.get(reverseRef),
      tx.get(responseRef),
      tx.get(usageRef),
      tx.get(senderBlockRef),
      tx.get(targetBlockRef),
      tx.get(senderPrivacyRef),
      tx.get(targetPrivacyRef),
      tx.get(riskActivityRef),
    ]);

    if (!senderSnap.exists) throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
    if (!targetSnap.exists) throw new functions.https.HttpsError("not-found", "Profile not found");
    if (senderBlock.exists || targetBlock.exists) {
      throw new functions.https.HttpsError("permission-denied", "Interest is unavailable for this member");
    }
    if (senderPrivacy.data()?.profileHidden === true || targetPrivacy.data()?.profileHidden === true) {
      throw new functions.https.HttpsError("permission-denied", "Interest is unavailable for this privacy relationship");
    }

    const sender = senderSnap.data() || {};
    const target = targetSnap.data() || {};
    if (!accountIsActive(sender.accountStatus) || !accountIsActive(target.accountStatus)) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Interest is unavailable while an account is not active"
      );
    }
    if (!discoveryActorReady(
      sender,
      senderPrivateSnap.data() || {},
      senderPreferencesSnap.data()
    )) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Complete your required profile and partner preferences before sending interests"
      );
    }
    if (!discoveryCandidateReady(target, targetPreferencesSnap.data())) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "This member is not yet available for interests"
      );
    }
    if (!genderCompatible(sender, target)) {
      throw new functions.https.HttpsError("failed-precondition", "This profile is outside mutual partner preferences");
    }
    if (target.stealthMode === true && !reverseSnap.exists) {
      throw new functions.https.HttpsError("permission-denied", "This profile is not accepting discovery interests");
    }

    if (!declinedInterestAllowsNewRequest(responseSnap.data()?.status, reverseSnap.exists)) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Interest cannot be sent to this member"
      );
    }

    if (outgoingSnap.exists) {
      return {
        success: true,
        mutual: reverseSnap.exists,
        alreadySent: true,
        interestsUsedToday: Number(usageSnap.data()?.count || 0),
      };
    }

    const safetyCount = safeUsageCount(riskActivity.data()?.interestCount);
    if (!usageAllowed(safetyCount, MAX_DAILY_INTERESTS_SAFETY)) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "Daily interest safety limit reached. Try again tomorrow."
      );
    }
    const nextSafetyCount = safetyCount + 1;
    tx.set(riskActivityRef, {
      interestCount: nextSafetyCount,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    if (crossesThreshold(safetyCount, HIGH_VOLUME_INTEREST_SIGNAL_THRESHOLD)) {
      tx.set(riskSignalRef, {
        highVolumeInterestDayCount: admin.firestore.FieldValue.increment(1),
        lastHighVolumeInterestAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }

    let interestsUsedToday = Number(usageSnap.data()?.count || 0);
    if (!activePaidMembership(sender)) {
      if (interestsUsedToday >= FREE_DAILY_INTEREST_LIMIT) {
        throw new functions.https.HttpsError(
          "resource-exhausted",
          `Free members can send ${FREE_DAILY_INTEREST_LIMIT} interests per day`
        );
      }
      interestsUsedToday += 1;
      tx.set(usageRef, {
        kind: "interest",
        day: usageDay,
        count: interestsUsedToday,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: false });
    }

    tx.set(outgoingRef, {
      fromUid: senderUid,
      toUid: targetUid,
      isSuperLike,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });
    tx.delete(responseRef);

    const mutual = reverseSnap.exists;
    if (mutual) {
      tx.set(matchRef, {
        users: [senderUid, targetUid].sort(),
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        lastActivity: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: false });
    }

    return { success: true, mutual, alreadySent: false, interestsUsedToday };
  });
});

/** Withdraw only a still-pending outgoing interest. */
export const withdrawInterest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const senderUid = context.auth?.uid;
  if (!senderUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const targetUid = requireUid(data?.targetUid, "target profile");
  if (targetUid === senderUid) throw new functions.https.HttpsError("invalid-argument", "Invalid target profile");

  const outgoingRef = db.collection("interests").doc(`${senderUid}_${targetUid}`);
  const reverseRef = db.collection("interests").doc(`${targetUid}_${senderUid}`);
  const matchRef = db.collection("matches").doc(matchId(senderUid, targetUid));

  await db.runTransaction(async (tx) => {
    const [outgoing, reverse, match] = await Promise.all([
      tx.get(outgoingRef),
      tx.get(reverseRef),
      tx.get(matchRef),
    ]);
    if (!outgoing.exists) return;
    if (outgoing.data()?.fromUid !== senderUid || outgoing.data()?.toUid !== targetUid) {
      throw new functions.https.HttpsError("permission-denied", "Interest does not belong to this account");
    }
    if (reverse.exists || match.exists) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "A mutual match cannot be withdrawn as a pending interest"
      );
    }
    tx.delete(outgoingRef);
  });

  return { success: true };
});

export const declineInterest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const recipientUid = context.auth?.uid;
  if (!recipientUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const senderUid = requireUid(data?.senderUid, "sender profile");
  if (senderUid === recipientUid) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid sender profile");
  }

  const incomingRef = db.collection("interests").doc(`${senderUid}_${recipientUid}`);
  const reverseRef = db.collection("interests").doc(`${recipientUid}_${senderUid}`);
  const matchRef = db.collection("matches").doc(matchId(senderUid, recipientUid));

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
