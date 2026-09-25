import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { db, getFcmToken, messaging } from "./shared";

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

async function persistNotificationOnce(
  notificationId: string,
  payload: NotificationPayload
): Promise<boolean> {
  const ref = db.collection("notifications").doc(notificationId);
  return db.runTransaction(async (tx) => {
    const existing = await tx.get(ref);
    if (existing.exists) return false;
    tx.create(ref, {
      ...payload,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      readAt: null,
    });
    return true;
  });
}

async function deliverPersistedNotification(
  notificationId: string,
  payload: NotificationPayload,
  channelId: string
): Promise<void> {
  const created = await persistNotificationOnce(notificationId, payload);
  if (!created) return;

  const fcmToken = await getFcmToken(payload.userId);
  if (!fcmToken) return;

  await messaging.send({
    token: fcmToken,
    data: {
      type: payload.type.toLowerCase(),
      title: payload.title,
      body: payload.body,
      recipient_uid: payload.userId,
      notification_id: notificationId,
      entity_type: payload.entityType,
      entity_id: payload.entityId,
      deep_link: payload.deepLink,
      ...(payload.fromFirebaseUid ? {
        user_id: payload.fromFirebaseUid,
        peer_uid: payload.fromFirebaseUid,
      } : {}),
    },
    android: { priority: "high", notification: { channelId } },
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
        deepLink: `matrimonyconnect://match?uid=${encodeURIComponent(fromUid)}`,
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
          deepLink: `matrimonyconnect://match?uid=${encodeURIComponent(uid2)}`,
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
          deepLink: `matrimonyconnect://match?uid=${encodeURIComponent(uid1)}`,
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
        deepLink: `matrimonyconnect://chat?thread=${encodeURIComponent(context.params.threadId)}&peer=${encodeURIComponent(fromFirebaseUid)}`,
        fromFirebaseUid,
      },
      "match_messages"
    );
  });
