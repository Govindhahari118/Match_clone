import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {
  db,
  persistAndSendNotification,
  requireAppCheck,
} from "./shared";

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
    if (typeof priorUid === "string" && priorUid && priorUid !== uid) {
      tx.delete(db.collection("fcmTokens").doc(priorUid).collection("devices").doc(deviceId));
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

async function deliverPersistedNotification(
  notificationId: string,
  payload: NotificationPayload,
  _channelId: string
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

    await deliverPersistedNotification(
      `interest_${context.params.interestId}_${toUid}`,
      {
        userId: toUid,
        type: "INTEREST",
        title: "New interest",
        body: "Someone is interested in your profile. Open the app to view it.",
        entityType: "profile",
        entityId: fromUid,
        deepLink: "matrimonyconnect://interests",
        fromFirebaseUid: fromUid,
      },
      "match_interests"
    );
  });

export const onMatchCreated = functions.firestore
  .document("matches/{matchId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    const users = data.users as string[];
    if (!users || users.length !== 2) return;

    const [uid1, uid2] = users;
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
          deepLink: "matrimonyconnect://matches",
          fromFirebaseUid: uid2,
        },
        "match_matches"
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
          deepLink: "matrimonyconnect://matches",
          fromFirebaseUid: uid1,
        },
        "match_matches"
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

    await deliverPersistedNotification(
      `message_${context.params.threadId}_${context.params.messageId}_${toFirebaseUid}`,
      {
        userId: toFirebaseUid,
        type: "MESSAGE",
        title: "New message",
        body: "Open the app to view your message.",
        entityType: "chat",
        entityId: context.params.threadId,
        deepLink: "matrimonyconnect://notifications",
        fromFirebaseUid,
      },
      "match_messages"
    );
  });
