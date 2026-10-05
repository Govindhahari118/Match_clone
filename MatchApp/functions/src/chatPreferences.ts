import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { canonicalChatThreadId } from "./chatIdentityPolicy";
import { db, requireAppCheck } from "./shared";

function cleanTargetUid(value: unknown, callerUid: string): string {
  const uid = typeof value === "string" ? value.trim() : "";
  if (!uid || uid === callerUid || uid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid conversation member");
  }
  return uid;
}

export const setChatThreadPreferences = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = cleanTargetUid(data?.targetUid, uid);
  const muted = data?.muted === true;
  const archived = data?.archived === true;
  const threadId = canonicalChatThreadId(uid, targetUid);
  const threadRef = db.collection("chats").doc(threadId);
  const prefRef = db.collection("chatPreferences").doc(uid).collection("threads").doc(threadId);

  await db.runTransaction(async (tx) => {
    const thread = await tx.get(threadRef);
    const participants = Array.isArray(thread.data()?.participantUids) ?
      thread.data()?.participantUids : [];
    if (!thread.exists || participants.length !== 2 ||
        !participants.includes(uid) || !participants.includes(targetUid)) {
      throw new functions.https.HttpsError("failed-precondition", "Conversation is unavailable");
    }

    tx.set(prefRef, {
      ownerUid: uid,
      threadId,
      peerUid: targetUid,
      muted,
      archived,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });
  });

  return { success: true, threadId, muted, archived };
});
