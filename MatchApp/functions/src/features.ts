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

function indianMobile(value: unknown): string {
  const result = text(value, "phone", 10);
  if (!/^[6-9][0-9]{9}$/.test(result)) {
    throw new functions.https.HttpsError("invalid-argument", "Enter a valid 10-digit Indian mobile number");
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

async function assertMutualMatch(uid: string, targetUid: string, message: string): Promise<void> {
  const match = await db.collection("matches").doc(pairId(uid, targetUid)).get();
  if (!match.exists) throw new functions.https.HttpsError("failed-precondition", message);
}

export const registerForEvent = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const eventId = text(data?.eventId, "event id", 128);
  const eventRef = db.collection("events").doc(eventId);
  const registrationRef = db.collection("eventRegistrations").doc(`${uid}_${eventId}`);

  return db.runTransaction(async (tx) => {
    const [event, existing] = await Promise.all([tx.get(eventRef), tx.get(registrationRef)]);
    if (!event.exists) throw new functions.https.HttpsError("not-found", "Event not found");
    if (existing.exists) {
      return { success: true, registrationId: registrationRef.id, alreadyRegistered: true };
    }
    const dateMillis = Number(event.data()?.dateMillis || 0);
    if (Number.isFinite(dateMillis) && dateMillis > 0 && dateMillis < Date.now()) {
      throw new functions.https.HttpsError("failed-precondition", "Registration for this event has closed");
    }
    const capacity = Number(event.data()?.capacity || 0);
    const attendees = Number(event.data()?.attendees || 0);
    if (Number.isFinite(capacity) && capacity > 0 && attendees >= capacity) {
      throw new functions.https.HttpsError("resource-exhausted", "This event is full");
    }
    tx.set(registrationRef, {
      uid,
      eventId,
      registeredAt: admin.firestore.FieldValue.serverTimestamp(),
    });
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
  const normalizedHash = Buffer.from(referredEmail).toString("base64url");
  const ref = db.collection("referrals").doc(`${uid}_${normalizedHash}`);
  const existing = await ref.get();
  if (existing.exists) {
    return { success: true, referralId: ref.id, alreadyReferred: true };
  }
  await ref.set({
    referrerUid: uid,
    referredEmail,
    status: "pending",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { success: true, referralId: ref.id, alreadyReferred: false };
});

/** A callback lead: selected RM package is a preference, not an entitlement or payment grant. */
export const requestRelationshipManager = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const user = await db.collection("users").doc(uid).get();
  if (!user.exists) throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
  const ref = db.collection("rmRequests").doc(uid);
  const name = text(data?.name, "name", 100);
  const phone = indianMobile(data?.phone);
  const preferences = text(data?.preferences, "preferences", 2000, false);
  const plan = choice(data?.plan, "relationship manager package", RM_PLANS);

  const response = await db.runTransaction(async (tx) => {
    const current = await tx.get(ref);
    const status = String(current.data()?.status || "");
    if (current.exists && status !== "closed" && status !== "cancelled") {
      return { requestId: ref.id, alreadyRequested: true, status };
    }
    tx.set(ref, {
      uid,
      name,
      phone,
      preferences,
      plan,
      status: "pending",
      assignedRM: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });
    return { requestId: ref.id, alreadyRequested: false, status: "pending" };
  });
  return { success: true, ...response };
});

/** Package selection records a service request only; it does not assert payment or verification. */
export const requestBackgroundCheck = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const targetUid = text(data?.targetUid, "target profile", 128);
  const selectedPackage = choice(data?.plan, "background check package", BACKGROUND_CHECK_PACKAGES);
  await assertRelationshipAvailable(uid, targetUid);
  await assertMutualMatch(uid, targetUid, "Background-check requests require a mutual match");
  const target = await db.collection("users").doc(targetUid).get();
  const targetProfileId = String(target.data()?.matrimonyId || "");
  const ref = db.collection("backgroundChecks").doc(`${uid}_${targetUid}`);
  const response = await db.runTransaction(async (tx) => {
    const existing = await tx.get(ref);
    const status = String(existing.data()?.status || "");
    if (existing.exists && status !== "cancelled") {
      return { requestId: ref.id, alreadyRequested: true, status: status || "submitted" };
    }
    tx.set(ref, {
      requestedBy: uid,
      targetUid,
      targetProfileId,
      plan: selectedPackage,
      status: "submitted",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });
    return { requestId: ref.id, alreadyRequested: false, status: "submitted" };
  });
  return { success: true, ...response };
});

export const requestSecureCall = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const targetUid = text(data?.targetUid, "target profile", 128);
  const type = text(data?.type, "call type", 32).toLowerCase();
  if (!CALL_TYPES.has(type)) {
    throw new functions.https.HttpsError("invalid-argument", "Unsupported call type");
  }
  const scheduledAt = text(data?.scheduledAt, "scheduled time", 100, false);
  await assertRelationshipAvailable(uid, targetUid);
  await assertMutualMatch(uid, targetUid, "Secure calls require a mutual match");
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
    if (current.toUid !== uid) {
      throw new functions.https.HttpsError("permission-denied", "Only the recipient can respond");
    }
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
    return {
      success: true,
      alreadyClaimed: false,
      coins,
      streak,
      rewardCoins,
      lastClaimedDay: today,
    };
  });
});

/**
 * Reward redemption uses the same private boost entitlement source as Google Play. A public user
 * profile field can therefore never manufacture ranking priority.
 */
export const redeemReward = functions.https.onCall(async (data, context) => {
  const uid = authUid(context);
  const rewardId = text(data?.rewardId, "reward", 64);
  if (rewardId !== "boost") {
    throw new functions.https.HttpsError("invalid-argument", "Reward is not available");
  }
  const cost = 100;
  const durationMs = 60 * 60 * 1000;
  const rewardRef = db.collection("rewards").doc(uid);
  const userRef = db.collection("users").doc(uid);
  const subscriptionRef = db.collection("subscriptions").doc(uid);
  const redemptionRef = rewardRef.collection("redemptions").doc();

  return db.runTransaction(async (tx) => {
    const [reward, user, subscription] = await Promise.all([
      tx.get(rewardRef),
      tx.get(userRef),
      tx.get(subscriptionRef),
    ]);
    if (!user.exists) {
      throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
    }
    const coins = Number(reward.data()?.coins || 0);
    if (coins < cost) {
      throw new functions.https.HttpsError("failed-precondition", "Insufficient coins");
    }
    const currentBoost = Number(subscription.data()?.boostUntil || 0);
    const boostUntil = Math.max(
      Date.now(),
      Number.isFinite(currentBoost) ? currentBoost : 0
    ) + durationMs;
    const now = admin.firestore.FieldValue.serverTimestamp();

    tx.set(rewardRef, { coins: coins - cost, updatedAt: now }, { merge: true });
    tx.set(subscriptionRef, { boostUntil, boostUpdatedAt: now }, { merge: true });
    tx.set(redemptionRef, {
      rewardId,
      cost,
      boostUntil,
      redeemedAt: now,
    });
    return { success: true, coins: coins - cost, boostUntil };
  });
});
