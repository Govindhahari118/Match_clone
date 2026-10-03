import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {
  notificationActionFor,
  notificationDeepLink,
} from "./notificationLinkPolicy";
import {
  db,
  persistAndSendNotification,
  requireAppCheck,
} from "./shared";
import { deviceAccountSwitchIsReviewSignal } from "./riskSignalPolicy";

type NotificationPayload = {
  userId: string;
  type: string;
  title: string;
  body: string;
  entityType: string;
  entityId: string;
  deepLink: string;
  fromFirebaseUid?: string;
};

function requireDeviceId(value: unknown): string {
  const deviceId = typeof value === "string" ? value.trim() : "";
  if (!/^[A-Za-z0-9_-]{16,128}$/.test(deviceId)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid device identity");
  }
  return deviceId;
}

function requireFcmToken(value: unknown): string {
  const token = typeof value === "string" ? value.trim() : "";
  if (token.length < 20 || token.length > 4096) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid notification token");
  }
  return token;
}

/**
 * Register exactly this authenticated app installation. A device identity can belong to only one
 * signed-in account at a time; registering after an account switch atomically removes the prior
 * account's device record.
 */
export const registerFcmDevice = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const deviceId = requireDeviceId(data?.deviceId);
  const token = requireFcmToken(data?.token);
  const appVersion = typeof data?.appVersion === "string" ?
    data.appVersion.trim().slice(0, 64) : "";

  const deviceRef = db.collection("fcmTokens").doc(uid).collection("devices").doc(deviceId);
  const ownerRef = db.collection("fcmDeviceOwners").doc(deviceId);

  await db.runTransaction(async (tx) => {
    const owner = await tx.get(ownerRef);
    const priorUid = owner.data()?.uid;
    if (deviceAccountSwitchIsReviewSignal(priorUid, uid)) {
      tx.delete(db.collection("fcmTokens").doc(String(priorUid)).collection("devices").doc(deviceId));
      tx.set(db.collection("riskSignals").doc(uid), {
        sharedDeviceAccountSwitchCount: admin.firestore.FieldValue.increment(1),
        lastSharedDeviceSignalAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }

    tx.set(deviceRef, {
      uid,
      deviceId,
      token,
      platform: "android",
      appVersion,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    tx.set(ownerRef, {
      uid,
      deviceId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  return { registered: true };
});

/** Revoke only the current authenticated installation, not the user's other signed-in devices. */
export const revokeFcmDevice = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const deviceId = requireDeviceId(data?.deviceId);
  const deviceRef = db.collection("fcmTokens").doc(uid).collection("devices").doc(deviceId);
  const ownerRef = db.collection("fcmDeviceOwners").doc(deviceId);

  await db.runTransaction(async (tx) => {
    const owner = await tx.get(ownerRef);
    tx.delete(deviceRef);
    if (owner.data()?.uid === uid) tx.delete(ownerRef);
  });

  return { revoked: true };
});

function preferenceFor(type: string): "interests" | "matches" | "messages" | "system" {
  if (type === "INTEREST") return "interests";
  if (type === "MATCH") return "matches";
  if (type === "MESSAGE") return "messages";
  return "system";
}

async function relationshipEventStillVisible(
  eventRef: FirebaseFirestore.DocumentReference,
  uidA: string,
  uidB: string
): Promise<boolean> {
  const [event, blockAB, blockBA, privacyAB, privacyBA] = await Promise.all([
    eventRef.get(),
    db.collection("blocks").doc(uidA).collection("blocked").doc(uidB).get(),
    db.collection("blocks").doc(uidB).collection("blocked").doc(uidA).get(),
    db.collection("privacyRelations").doc(uidA).collection("members").doc(uidB).get(),
    db.collection("privacyRelations").doc(uidB).collection("members").doc(uidA).get(),
  ]);
  return event.exists &&
    !blockAB.exists &&
    !blockBA.exists &&
    privacyAB.data()?.profileHidden !== true &&
    privacyBA.data()?.profileHidden !== true;
}

async function chatNotificationStillAllowed(
  threadId: string,
  fromUid: string,
  toUid: string
): Promise<boolean> {
  const [thread, blockAB, blockBA] = await Promise.all([
    db.collection("chats").doc(threadId).get(),
    db.collection("blocks").doc(fromUid).collection("blocked").doc(toUid).get(),
    db.collection("blocks").doc(toUid).collection("blocked").doc(fromUid).get(),
  ]);
  const participants = thread.data()?.participantUids;
  return thread.exists &&
    Array.isArray(participants) &&
    participants.length === 2 &&
    participants.includes(fromUid) &&
    participants.includes(toUid) &&
    !blockAB.exists &&
    !blockBA.exists;
}

async function deliverPersistedNotification(
  notificationId: string,
  payload: NotificationPayload
): Promise<void> {
  const pushType = payload.type === "INTEREST" ? "interest_received" :
    payload.type === "MATCH" ? "mutual_match" :
      payload.type === "MESSAGE" ? "message" : payload.type.toLowerCase();

  await persistAndSendNotification({
    notificationId,
    userId: payload.userId,
    type: payload.type,
    title: payload.title,
    body: payload.body,
    entityType: payload.entityType,
    entityId: payload.entityId,
    deepLink: payload.deepLink,
    action: notificationActionFor(payload.type),
    pushType,
    preferenceKey: preferenceFor(payload.type),
    priority: "high",
    fromFirebaseUid: payload.fromFirebaseUid,
  });
}

export const onInterestCreated = functions.firestore
  .document("interests/{interestId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    const fromUid = data.fromUid as string;
    const toUid = data.toUid as string;
    if (!fromUid || !toUid) return;
    if (!await relationshipEventStillVisible(snap.ref, fromUid, toUid)) return;

    await deliverPersistedNotification(
      `interest_${context.params.interestId}_${toUid}`,
      {
        userId: toUid,
        type: "INTEREST",
        title: "New interest",
        body: "Someone is interested in your profile. Open the app to view it.",
        entityType: "profile",
        entityId: fromUid,
        deepLink: notificationDeepLink("interests", functions.config().app_links?.host),
        fromFirebaseUid: fromUid,
      }
    );
  });

export const onMatchCreated = functions.firestore
  .document("matches/{matchId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    const users = data.users as string[];
    if (!users || users.length !== 2) return;

    const [uid1, uid2] = users;
    if (!await relationshipEventStillVisible(snap.ref, uid1, uid2)) return;
    await Promise.allSettled([
      deliverPersistedNotification(
        `match_${context.params.matchId}_${uid1}`,
        {
          userId: uid1,
          type: "MATCH",
          title: "New mutual match",
          body: "You have a new mutual match. Open the app to view the profile.",
          entityType: "profile",
          entityId: uid2,
          deepLink: notificationDeepLink("matches", functions.config().app_links?.host),
          fromFirebaseUid: uid2,
        }
      ),
      deliverPersistedNotification(
        `match_${context.params.matchId}_${uid2}`,
        {
          userId: uid2,
          type: "MATCH",
          title: "New mutual match",
          body: "You have a new mutual match. Open the app to view the profile.",
          entityType: "profile",
          entityId: uid1,
          deepLink: notificationDeepLink("matches", functions.config().app_links?.host),
          fromFirebaseUid: uid1,
        }
      ),
    ]);
  });

export const onNewMessage = functions.firestore
  .document("chats/{threadId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    const fromFirebaseUid = data.fromFirebaseUid as string | undefined;
    const toFirebaseUid = data.toFirebaseUid as string | undefined;
    if (!fromFirebaseUid || !toFirebaseUid) return;
    if (!await chatNotificationStillAllowed(
      context.params.threadId,
      fromFirebaseUid,
      toFirebaseUid
    )) return;

    await deliverPersistedNotification(
      `message_${context.params.threadId}_${context.params.messageId}_${toFirebaseUid}`,
      {
        userId: toFirebaseUid,
        type: "MESSAGE",
        title: "New message",
        body: "Open the app to view your message.",
        entityType: "chat",
        entityId: context.params.threadId,
        deepLink: notificationDeepLink("notifications", functions.config().app_links?.host),
        fromFirebaseUid,
      }
    );
  });
