import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

const RM_PLANS = new Set(["Silver RM", "Gold RM", "Platinum RM"]);
const BACKGROUND_CHECK_PACKAGES = new Set(["Basic Verify", "Professional", "Premium 360°"]);
const CALL_TYPES = new Set(["voice", "video", "virtual_meet"]);

function authUid(context: functions.https.CallableContext): string {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  return uid;
}

function containsDisallowedControlCharacter(value: string): boolean {
  for (let index = 0; index < value.length; index += 1) {
    const code = value.charCodeAt(index);
    if ((code >= 0 && code <= 8) || code === 11 || code === 12 || (code >= 14 && code <= 31)) {
      return true;
    }
  }
  return false;
}

function text(value: unknown, field: string, maxLength: number, required = true): string {
  const result = typeof value === "string" ? value.trim() : "";
  if ((required && !result) || result.length > maxLength || containsDisallowedControlCharacter(result)) {
    throw new functions.https.HttpsError("invalid-argument", `Invalid ${field}`);
  }
  return result;
}

function email(value: unknown): string {
  const result = text(value, "email", 254).toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(result)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid email");
  }
  return result;
}

function choice(value: unknown, field: string, allowed: Set<string>): string {
  const result = text(value, field, 80);
  if (!allowed.has(result)) {
    throw new functions.https.HttpsError("invalid-argument", `Unsupported ${field}`);
  }
  return result;
}

function pairId(uidA: string, uidB: string): string {
  return [uidA, uidB].sort().join("_");
}

async function assertRelationshipAvailable(uid: string, targetUid: string): Promise<void> {
  if (uid === targetUid) {
    throw new functions.https.HttpsError("invalid-argument", "Target account must be another member");
  }
  const [target, fromBlock, toBlock, fromPrivacy, toPrivacy] = await Promise.all([
    db.collection("users").doc(targetUid).get(),
    db.collection("blocks").doc(uid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(uid).get(),
    db.collection("privacyRelations").doc(uid).collection("members").doc(targetUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(uid).get(),
  ]);
  if (!target.exists) throw new functions.https.HttpsError("not-found", "Profile not found");
  if (fromBlock.exists || toBlock.exists ||
      fromPrivacy.data()?.profileHidden === true || toPrivacy.data()?.profileHidden === true) {
    throw new functions.https.HttpsError("permission-denied", "Action unavailable for this privacy relationship");
  }
}

export const registerForEvent = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const eventId = text(data?.eventId, "event id", 128);
  const eventRef = db.collection("events").doc(eventId);
  const registrationRef = db.collection("eventRegistrations").doc(`${uid}_${eventId}`);

  return db.runTransaction(async (tx) => {
    const [event, existing] = await Promise.all([tx.get(eventRef), tx.get(registrationRef)]);
    if (!event.exists) throw new functions.https.HttpsError("not-found", "Event not found");
    if (existing.exists) return { success: true, registrationId: registrationRef.id, alreadyRegistered: true };
    const dateMillis = Number(event.data()?.dateMillis || 0);
    if (Number.isFinite(dateMillis) && dateMillis > 0 && dateMillis < Date.now()) {
      throw new functions.https.HttpsError("failed-precondition", "Registration for this event has closed");
    }
    tx.set(registrationRef, { uid, eventId, registeredAt: admin.firestore.FieldValue.serverTimestamp() });
    tx.update(eventRef, { attendees: admin.firestore.FieldValue.increment(1) });
    return { success: true, registrationId: registrationRef.id, alreadyRegistered: false };
  });
});

export const bookCounselling = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const ref = db.collection("counsellingBookings").doc();
  await ref.set({
    uid,
    counsellor: text(data?.counsellor, "counsellor", 100),
    sessionType: text(data?.sessionType, "session type", 80),
    mode: text(data?.mode, "session mode", 40),
    date: text(data?.date, "date", 40),
    time: text(data?.time, "time", 40),
    status: "confirmed",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { success: true, bookingId: ref.id };
});

export const recordReferral = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const referredEmail = email(data?.referredEmail);
  const privateDoc = await db.collection("userPrivate").doc(uid).get();
  const ownEmail = String(privateDoc.data()?.email || "").trim().toLowerCase();
  if (ownEmail && ownEmail === referredEmail) {
    throw new functions.https.HttpsError("invalid-argument", "You cannot refer your own account");
  }
  const ref = db.collection("referrals").doc();
  await ref.set({
    referrerUid: uid,
    referredEmail,
    status: "pending",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { success: true, referralId: ref.id };
});

/** A callback lead: selected RM package is a preference, not an entitlement or payment grant. */
export const requestRelationshipManager = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const user = await db.collection("users").doc(uid).get();
  if (!user.exists) throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
  const ref = db.collection("rmRequests").doc();
  await ref.set({
    uid,
    name: text(data?.name, "name", 100),
    phone: text(data?.phone, "phone", 20),
    preferences: text(data?.preferences, "preferences", 2000, false),
    plan: choice(data?.plan, "relationship manager package", RM_PLANS),
    status: "pending",
    assignedRM: null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { success: true, requestId: ref.id };
});

/** Package selection records a service request only; it does not assert payment or verification. */
export const requestBackgroundCheck = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const targetUid = text(data?.targetUid, "target profile", 128);
  const packageName = choice(data?.plan, "background check package", BACKGROUND_CHECK_PACKAGES);
  await assertRelationshipAvailable(uid, targetUid);
  const ref = db.collection("backgroundChecks").doc();
  await ref.set({
    requestedBy: uid,
    targetUid,
    plan: packageName,
    status: "submitted",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { success: true, requestId: ref.id };
});

export const requestSecureCall = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const targetUid = text(data?.targetUid, "target profile", 128);
  const type = text(data?.type, "call type", 32).toLowerCase();
  if (!CALL_TYPES.has(type)) throw new functions.https.HttpsError("invalid-argument", "Unsupported call type");
  const scheduledAt = text(data?.scheduledAt, "scheduled time", 100, false);
  await assertRelationshipAvailable(uid, targetUid);
  const match = await db.collection("matches").doc(pairId(uid, targetUid)).get();
  if (!match.exists) throw new functions.https.HttpsError("failed-precondition", "Secure calls require a mutual match");
  const ref = db.collection("callRequests").doc();
  await ref.set({
    fromUid: uid,
    toUid: targetUid,
    type,
    scheduledAt,
    status: "requested",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { success: true, requestId: ref.id };
});

export const respondToSecureCall = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const requestId = text(data?.requestId, "request id", 128);
  const accept = data?.accept === true;
  const ref = db.collection("callRequests").doc(requestId);
  await db.runTransaction(async (tx) => {
    const request = await tx.get(ref);
    if (!request.exists) throw new functions.https.HttpsError("not-found", "Call request not found");
    const current = request.data() || {};
    if (current.toUid !== uid) throw new functions.https.HttpsError("permission-denied", "Only the recipient can respond");
    if (current.status !== "requested") {
      throw new functions.https.HttpsError("failed-precondition", "Call request has already been handled");
    }
    tx.update(ref, {
      status: accept ? "accepted" : "declined",
      respondedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });
  return { success: true };
});

export const joinCommunity = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const communityId = text(data?.communityId, "community id", 128);
  const communityRef = db.collection("communities").doc(communityId);
  const memberRef = communityRef.collection("members").doc(uid);
  return db.runTransaction(async (tx) => {
    const [community, member] = await Promise.all([tx.get(communityRef), tx.get(memberRef)]);
    if (!community.exists) throw new functions.https.HttpsError("not-found", "Community not found");
    if (member.exists) return { success: true, alreadyJoined: true };
    tx.set(memberRef, {
      uid,
      memberUid: uid,
      joinedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.update(communityRef, { memberCount: admin.firestore.FieldValue.increment(1) });
    return { success: true, alreadyJoined: false };
  });
});

/** Reward ledger is read through a callable so the collection can remain client-inaccessible. */
export const getRewardsState = functions.https.onCall(async (_data, context) => {
  const uid = authUid(context);
  const snap = await db.collection("rewards").doc(uid).get();
  const value = snap.data() || {};
  return {
    coins: Math.max(0, Number(value.coins || 0)),
    streak: Math.max(0, Number(value.streak || 0)),
    lastClaimedDay: typeof value.lastClaimedDay === "string" ? value.lastClaimedDay : "",
  };
});

export const claimDailyReward = functions.https.onCall(async (_data, context) => {
  const uid = authUid(context);
  const rewardRef = db.collection("rewards").doc(uid);
  const today = new Date().toISOString().slice(0, 10);
  const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  return db.runTransaction(async (tx) => {
    const snapshot = await tx.get(rewardRef);
    const current = snapshot.data() || {};
    if (current.lastClaimedDay === today) {
      return {
        success: true,
        alreadyClaimed: true,
        coins: Number(current.coins || 0),
        streak: Number(current.streak || 0),
        lastClaimedDay: today,
      };
    }
    const previousStreak = Number(current.streak || 0);
    const streak = current.lastClaimedDay === yesterday ? previousStreak + 1 : 1;
    const rewardCoins = Math.min(10 + (streak - 1) * 5, 40);
    const coins = Number(current.coins || 0) + rewardCoins;
    tx.set(rewardRef, {
      coins,
      streak,
      lastClaimedDay: today,
      lastClaimed: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    return { success: true, alreadyClaimed: false, coins, streak, rewardCoins, lastClaimedDay: today };
  });
});

export const redeemReward = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const rewardId = text(data?.rewardId, "reward", 64);
  if (rewardId !== "boost") throw new functions.https.HttpsError("invalid-argument", "Reward is not available");
  const cost = 100;
  const durationMs = 60 * 60 * 1000;
  const rewardRef = db.collection("rewards").doc(uid);
  const userRef = db.collection("users").doc(uid);
  const redemptionRef = rewardRef.collection("redemptions").doc();
  return db.runTransaction(async (tx) => {
    const [reward, user] = await Promise.all([tx.get(rewardRef), tx.get(userRef)]);
    if (!user.exists) throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
    const coins = Number(reward.data()?.coins || 0);
    if (coins < cost) throw new functions.https.HttpsError("failed-precondition", "Insufficient coins");
    const currentBoost = Number(user.data()?.boostActiveUntil || 0);
    const boostUntil = Math.max(Date.now(), Number.isFinite(currentBoost) ? currentBoost : 0) + durationMs;
    tx.set(rewardRef, { coins: coins - cost, updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
    tx.update(userRef, { boostActiveUntil: boostUntil, updatedAt: admin.firestore.FieldValue.serverTimestamp() });
    tx.set(redemptionRef, {
      rewardId,
      cost,
      boostUntil,
      redeemedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    return { success: true, coins: coins - cost, boostUntil };
  });
});
