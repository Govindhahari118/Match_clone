import * as functions from "firebase-functions/v1";
import { getFcmToken, messaging } from "./shared";

export const onInterestCreated = functions.firestore
  .document("interests/{interestId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    const fromUid = data.fromUid as string;
    const toUid = data.toUid as string;
    if (!fromUid || !toUid) return;

    const fcmToken = await getFcmToken(toUid);
    if (!fcmToken) return;

    // Keep relationship identity off the lock screen. The authenticated app resolves the sender.
    await messaging.send({
      token: fcmToken,
      data: {
        type: "interest_received",
        title: "New interest",
        body: "Someone is interested in your profile. Open the app to view it.",
        user_id: fromUid,
      },
      android: { priority: "high", notification: { channelId: "match_interests" } },
    });
  });

export const onMatchCreated = functions.firestore
  .document("matches/{matchId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    const users = data.users as string[];
    if (!users || users.length !== 2) return;

    const [uid1, uid2] = users;
    const [token1, token2] = await Promise.all([getFcmToken(uid1), getFcmToken(uid2)]);
    const sends: Promise<string>[] = [];
    if (token1) {
      sends.push(messaging.send({
        token: token1,
        data: {
          type: "mutual_match",
          title: "New mutual match",
          body: "You have a new mutual match. Open the app to view the profile.",
          user_id: uid2,
        },
        android: { priority: "high", notification: { channelId: "match_matches" } },
      }));
    }
    if (token2) {
      sends.push(messaging.send({
        token: token2,
        data: {
          type: "mutual_match",
          title: "New mutual match",
          body: "You have a new mutual match. Open the app to view the profile.",
          user_id: uid1,
        },
        android: { priority: "high", notification: { channelId: "match_matches" } },
      }));
    }
    await Promise.allSettled(sends);
  });

export const onNewMessage = functions.firestore
  .document("chats/{threadId}/messages/{messageId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    const fromFirebaseUid = data.fromFirebaseUid as string | undefined;
    const toFirebaseUid = data.toFirebaseUid as string | undefined;
    if (!fromFirebaseUid || !toFirebaseUid) return;

    const fcmToken = await getFcmToken(toFirebaseUid);
    if (!fcmToken) return;

    await messaging.send({
      token: fcmToken,
      data: {
        type: "message",
        title: "New message",
        body: "Open the app to view your message.",
        peer_uid: fromFirebaseUid,
      },
      android: { priority: "high", notification: { channelId: "match_messages" } },
    });
  });