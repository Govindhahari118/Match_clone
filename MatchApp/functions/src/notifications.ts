import * as functions from "firebase-functions";
import { db, messaging } from "./shared";

// ── Interest Created → Notify target user ──────────────────────────────────
export const onInterestCreated = functions.firestore
  .document("interests/{interestId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    const fromUid = data.fromUid as string;
    const toUid = data.toUid as string;

    const senderDoc = await db.collection("users").doc(fromUid).get();
    const senderName = senderDoc.data()?.displayName || "Someone";

    const targetDoc = await db.collection("users").doc(toUid).get();
    const fcmToken = targetDoc.data()?.fcmToken as string | undefined;

    if (!fcmToken) return;

    await messaging.send({
      token: fcmToken,
      data: {
        type: "interest_received",
        title: "New Interest ❤️",
        body: `${senderName} is interested in your profile!`,
        user_id: fromUid,
      },
      android: {
        priority: "high",
        notification: { channelId: "match_interests" },
      },
    });
  });

// ── Match Created → Notify both users ──────────────────────────────────────
export const onMatchCreated = functions.firestore
  .document("matches/{matchId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    const users = data.users as string[];
    if (!users || users.length !== 2) return;

    const [uid1, uid2] = users;
    const [doc1, doc2] = await Promise.all([
      db.collection("users").doc(uid1).get(),
      db.collection("users").doc(uid2).get(),
    ]);

    const name1 = doc1.data()?.displayName || "Someone";
    const name2 = doc2.data()?.displayName || "Someone";
    const token1 = doc1.data()?.fcmToken as string | undefined;
    const token2 = doc2.data()?.fcmToken as string | undefined;

    const sends: Promise<string>[] = [];
    if (token1) {
      sends.push(messaging.send({
        token: token1,
        data: {
          type: "mutual_match",
          title: "It's a Match! 🎉",
          body: `You and ${name2} have liked each other. Say hello!`,
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
          title: "It's a Match! 🎉",
          body: `You and ${name1} have liked each other. Say hello!`,
          user_id: uid1,
        },
        android: { priority: "high", notification: { channelId: "match_matches" } },
      }));
    }
    await Promise.all(sends);
  });

// ── New Chat Message → Notify recipient ────────────────────────────────────
export const onNewMessage = functions.firestore
  .document("chats/{threadId}/messages/{messageId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    const fromFirebaseUid = data.fromFirebaseUid as string | undefined;
    const toFirebaseUid = data.toFirebaseUid as string | undefined;
    const body = (data.body as string) || "";

    if (!fromFirebaseUid || !toFirebaseUid) return;

    const [senderDoc, recipientDoc] = await Promise.all([
      db.collection("users").doc(fromFirebaseUid).get(),
      db.collection("users").doc(toFirebaseUid).get(),
    ]);

    const senderName = senderDoc.data()?.displayName || "Someone";
    const fcmToken = recipientDoc.data()?.fcmToken as string | undefined;
    if (!fcmToken) return;

    const preview = body.length > 100 ? body.substring(0, 97) + "..." : body;

    await messaging.send({
      token: fcmToken,
      data: {
        type: "message",
        title: senderName,
        body: preview,
        peer_id: fromFirebaseUid,
      },
      android: { priority: "high", notification: { channelId: "match_messages" } },
    });
  });
