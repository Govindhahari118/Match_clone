import * as functions from "firebase-functions";
import { db, getFcmToken, messaging } from "./shared";

export const onInterestCreated = functions.firestore
  .document("interests/{interestId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    const fromUid = data.fromUid as string;
    const toUid = data.toUid as string;

    const senderDoc = await db.collection("users").doc(fromUid).get();
    const senderName = senderDoc.data()?.displayName || "Someone";
    const fcmToken = await getFcmToken(toUid);
    if (!fcmToken) return;

    await messaging.send({
      token: fcmToken,
      data: {
        type: "interest_received",
        title: "New Interest ❤️",
        body: `${senderName} is interested in your profile!`,
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
    const [doc1, doc2, token1, token2] = await Promise.all([
      db.collection("users").doc(uid1).get(),
      db.collection("users").doc(uid2).get(),
      getFcmToken(uid1),
      getFcmToken(uid2),
    ]);
    const name1 = doc1.data()?.displayName || "Someone";
    const name2 = doc2.data()?.displayName || "Someone";

    const sends: Promise<string>[] = [];
    if (token1) {
      sends.push(messaging.send({
        token: token1,
        data: { type: "mutual_match", title: "It's a Match! 🎉", body: `You and ${name2} have liked each other. Say hello!`, user_id: uid2 },
        android: { priority: "high", notification: { channelId: "match_matches" } },
      }));
    }
    if (token2) {
      sends.push(messaging.send({
        token: token2,
        data: { type: "mutual_match", title: "It's a Match! 🎉", body: `You and ${name1} have liked each other. Say hello!`, user_id: uid1 },
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

    const senderDoc = await db.collection("users").doc(fromFirebaseUid).get();
    const senderName = senderDoc.data()?.displayName || "Someone";
    const fcmToken = await getFcmToken(toFirebaseUid);
    if (!fcmToken) return;

    // Do not push encrypted/plain message contents onto a lock screen. The app fetches
    // the actual conversation after authentication.
    await messaging.send({
      token: fcmToken,
      data: {
        type: "message",
        title: senderName,
        body: "You have a new message",
        peer_uid: fromFirebaseUid,
      },
      android: { priority: "high", notification: { channelId: "match_messages" } },
    });
  });
