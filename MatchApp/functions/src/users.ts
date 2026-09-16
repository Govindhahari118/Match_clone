import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { createHash } from "crypto";
import {
  db,
  getFcmToken,
  messaging,
  releaseMatrimonyId,
  requireAppCheck,
  reserveMatrimonyId,
} from "./shared";

export const onUserCreate = functions.firestore
  .document("users/{uid}")
  .onCreate(async (snap, context) => {
    const uid = context.params.uid;
    const matrimonyId = await reserveMatrimonyId(uid);

    await snap.ref.update({
      matrimonyId,
      profileCompleteness: 0.1,
      verificationLevel: 1,
      subscriptionPlan: "FREE",
    });

    await db.collection("profileAnalytics").doc(uid).set({
      viewCount: 0,
      likeCount: 0,
      matchCount: 0,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

export const sendInactivityNudge = functions.pubsub
  .schedule("0 5 * * *")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = Date.now();
    const sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000;
    const fourteenDaysAgo = now - 14 * 24 * 60 * 60 * 1000;
    const nudge7 = await db.collection("users")
      .where("lastActiveAt", "<=", sevenDaysAgo)
      .where("lastActiveAt", ">", fourteenDaysAgo)
      .limit(100).get();

    const sends: Promise<unknown>[] = [];
    for (const doc of nudge7.docs) {
      sends.push((async () => {
        const token = await getFcmToken(doc.id);
        if (!token) return;
        await messaging.send({
          token,
          data: {
            type: "inactivity_nudge",
            title: "We miss you! 💝",
            body: "New profiles matching your preferences are waiting. Come back and explore!",
            recipient_uid: doc.id,
          },
          android: { priority: "normal", notification: { channelId: "match_system" } },
        });
      })());
    }
    await Promise.allSettled(sends);
    functions.logger.info(`Inactivity nudges processed: ${sends.length}`);
  });

export const sendProfileIncompleteD2 = functions.pubsub
  .schedule("0 4 * * *")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = Date.now();
    const twoDaysAgo = now - 2 * 24 * 60 * 60 * 1000;
    const threeDaysAgo = now - 3 * 24 * 60 * 60 * 1000;
    const incomplete = await db.collection("users")
      .where("profileCompleteness", "<", 0.5)
      .where("createdAt", "<=", twoDaysAgo)
      .where("createdAt", ">", threeDaysAgo)
      .limit(200).get();

    const sends = incomplete.docs.map(async (doc) => {
      const token = await getFcmToken(doc.id);
      if (!token) return;
      await messaging.send({
        token,
        data: {
          type: "profile_incomplete",
          title: "Complete your profile 📝",
          body: "Complete your profile to improve match quality and visibility.",
          recipient_uid: doc.id,
        },
        android: { priority: "normal", notification: { channelId: "match_system" } },
      });
    });
    await Promise.allSettled(sends);
  });

export const sendProfileIncompleteD7 = functions.pubsub
  .schedule("0 4 * * *")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = Date.now();
    const sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000;
    const eightDaysAgo = now - 8 * 24 * 60 * 60 * 1000;
    const incomplete = await db.collection("users")
      .where("profileCompleteness", "<", 0.6)
      .where("createdAt", "<=", sevenDaysAgo)
      .where("createdAt", ">", eightDaysAgo)
      .limit(200).get();

    const sends = incomplete.docs.map(async (doc) => {
      const token = await getFcmToken(doc.id);
      if (!token) return;
      await messaging.send({
        token,
        data: {
          type: "profile_incomplete",
          title: "Finish your profile 💡",
          body: "Add your remaining details and photo to improve match quality.",
          recipient_uid: doc.id,
        },
        android: { priority: "normal", notification: { channelId: "match_system" } },
      });
    });
    await Promise.allSettled(sends);
  });

const DELETE_BATCH_SIZE = 300;

async function deleteQuery(query: FirebaseFirestore.Query): Promise<number> {
  let deleted = 0;
  let hasMore = true;
  while (hasMore) {
    const snap = await query.limit(DELETE_BATCH_SIZE).get();
    if (snap.empty) {
      hasMore = false;
      continue;
    }
    const batch = db.batch();
    snap.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    deleted += snap.size;
    hasMore = snap.size === DELETE_BATCH_SIZE;
  }
  return deleted;
}

async function deleteCollection(path: string): Promise<number> {
  return deleteQuery(db.collection(path));
}

async function deleteChatThread(thread: FirebaseFirestore.QueryDocumentSnapshot): Promise<void> {
  await deleteQuery(thread.ref.collection("messages"));
  await thread.ref.delete();
}

export const deleteUserAccount = functions
  .runWith({ timeoutSeconds: 540, memory: "1GB" })
  .https.onCall(async (_data, context) => {
    requireAppCheck(context);
    const uid = context.auth?.uid;
    if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

    const requestRef = db.collection("deletionRequests").doc(uid);
    await requestRef.set({
      status: "PROCESSING",
      startedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    try {
      await deleteQuery(db.collection("interests").where("fromUid", "==", uid));
      await deleteQuery(db.collection("interests").where("toUid", "==", uid));
      await deleteQuery(db.collection("interestResponses").where("senderUid", "==", uid));
      await deleteQuery(db.collection("interestResponses").where("recipientUid", "==", uid));
      await deleteQuery(db.collection("matches").where("users", "array-contains", uid));
      await deleteQuery(db.collection("notifications").where("userId", "==", uid));
      await deleteQuery(db.collection("profileViews").where("viewerUid", "==", uid));
      await deleteQuery(db.collection("profileViews").where("viewedUid", "==", uid));
      await deleteQuery(db.collection("eventRegistrations").where("uid", "==", uid));
      await deleteQuery(db.collection("counsellingBookings").where("uid", "==", uid));
      await deleteQuery(db.collection("referrals").where("referrerUid", "==", uid));
      await deleteQuery(db.collection("rmRequests").where("uid", "==", uid));
      await deleteQuery(db.collection("backgroundChecks").where("requestedBy", "==", uid));
      await deleteQuery(db.collection("backgroundChecks").where("targetUid", "==", uid));
      await deleteQuery(db.collection("callRequests").where("fromUid", "==", uid));
      await deleteQuery(db.collection("callRequests").where("toUid", "==", uid));

      await deleteCollection(`savedSearches/${uid}/items`);
      await deleteQuery(db.collection("usernames").where("uid", "==", uid));
      await deleteCollection(`shortlists/${uid}/saved`);
      await deleteQuery(db.collectionGroup("saved").where("targetUid", "==", uid));
      await deleteCollection(`blocks/${uid}/blocked`);
      await deleteQuery(db.collectionGroup("blocked").where("blockedUid", "==", uid));
      await deleteCollection(`privacyRelations/${uid}/members`);
      await deleteQuery(db.collectionGroup("members").where("memberUid", "==", uid));
      await deleteCollection(`subscriptions/${uid}/usage`);
      await deleteCollection(`profileAnalytics/${uid}/weekly`);
      await deleteCollection(`sessions/${uid}/devices`);

      const chats = await db.collection("chats").where("participantUids", "array-contains", uid).get();
      const chatThreadIds = chats.docs.map((thread) => thread.id);
      for (const thread of chats.docs) await deleteChatThread(thread);

      const bucket = admin.storage().bucket();
      for (const prefix of ["photos", "videos", "voicebios", "verifications"]) {
        await bucket.deleteFiles({ prefix: `${prefix}/${uid}/` });
      }
      for (const threadId of chatThreadIds) {
        await bucket.deleteFiles({ prefix: `chat-media/${threadId}/` });
      }

      const singletonRefs = [
        db.collection("users").doc(uid),
        db.collection("userPrivate").doc(uid),
        db.collection("savedSearches").doc(uid),
        db.collection("shortlists").doc(uid),
        db.collection("blocks").doc(uid),
        db.collection("privacyRelations").doc(uid),
        db.collection("privacySettings").doc(uid),
        db.collection("subscriptions").doc(uid),
        db.collection("profileAnalytics").doc(uid),
        db.collection("notificationPrefs").doc(uid),
        db.collection("verifications").doc(uid),
        db.collection("verificationRequests").doc(uid),
        db.collection("rewards").doc(uid),
        db.collection("sessions").doc(uid),
        db.collection("fcmTokens").doc(uid),
        db.collection("userLocations").doc(uid),
      ];
      for (let i = 0; i < singletonRefs.length; i += DELETE_BATCH_SIZE) {
        const batch = db.batch();
        singletonRefs.slice(i, i + DELETE_BATCH_SIZE).forEach((ref) => batch.delete(ref));
        await batch.commit();
      }

      await releaseMatrimonyId(uid);
      await admin.auth().deleteUser(uid);

      await requestRef.set({
        status: "DELETED",
        completedAt: admin.firestore.FieldValue.serverTimestamp(),
        expireAt: admin.firestore.Timestamp.fromMillis(Date.now() + 30 * 24 * 60 * 60 * 1000),
      }, { merge: true });
      return { success: true };
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      functions.logger.error("Account deletion failed", { uid, error: message });
      await requestRef.set({
        status: "FAILED_RETRYABLE",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        errorCode: "cleanup-failed",
      }, { merge: true });
      throw new functions.https.HttpsError("internal", "Account deletion could not be completed. Please retry.");
    }
  });

export const recordProfileView = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const viewerUid = context.auth?.uid;
  if (!viewerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const viewedUid = typeof data?.viewedUid === "string" ? data.viewedUid.trim() : "";
  if (!viewedUid || viewedUid === viewerUid) {
    throw new functions.https.HttpsError("invalid-argument", "A valid target profile is required");
  }

  const [target, viewerBlocked, targetBlocked, targetPrivacy, viewerPrivacy] = await Promise.all([
    db.collection("users").doc(viewedUid).get(),
    db.collection("blocks").doc(viewerUid).collection("blocked").doc(viewedUid).get(),
    db.collection("blocks").doc(viewedUid).collection("blocked").doc(viewerUid).get(),
    db.collection("privacyRelations").doc(viewedUid).collection("members").doc(viewerUid).get(),
    db.collection("privacyRelations").doc(viewerUid).collection("members").doc(viewedUid).get(),
  ]);
  if (!target.exists) throw new functions.https.HttpsError("not-found", "Profile not found");
  if (
    target.data()?.stealthMode === true ||
    viewerBlocked.exists ||
    targetBlocked.exists ||
    targetPrivacy.data()?.profileHidden === true ||
    viewerPrivacy.data()?.profileHidden === true
  ) {
    throw new functions.https.HttpsError("permission-denied", "Profile view is unavailable for this privacy relationship");
  }

  const day = new Date().toISOString().slice(0, 10);
  const viewId = createHash("sha256").update(`${viewerUid}|${viewedUid}|${day}`).digest("hex");
  const viewRef = db.collection("profileViews").doc(viewId);

  const recorded = await db.runTransaction(async (tx) => {
    const existing = await tx.get(viewRef);
    if (existing.exists) return false;
    tx.set(viewRef, { viewerUid, viewedUid, viewedAt: Date.now() });
    return true;
  });

  return { recorded };
});

export const onProfileViewed = functions.firestore
  .document("profileViews/{viewId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    const viewedUid = data.viewedUid as string;
    const viewerUid = data.viewerUid as string;
    if (!viewedUid || !viewerUid || viewedUid === viewerUid) return;

    const [targetPrivacy, viewerPrivacy] = await Promise.all([
      db.collection("privacyRelations").doc(viewedUid).collection("members").doc(viewerUid).get(),
      db.collection("privacyRelations").doc(viewerUid).collection("members").doc(viewedUid).get(),
    ]);
    if (targetPrivacy.data()?.profileHidden === true || viewerPrivacy.data()?.profileHidden === true) {
      await snap.ref.delete();
      return;
    }

    await db.collection("users").doc(viewedUid).update({
      profileViewCount: admin.firestore.FieldValue.increment(1),
    });

    const token = await getFcmToken(viewedUid);
    if (!token) return;
    const viewerDoc = await db.collection("users").doc(viewerUid).get();
    const viewerName = viewerDoc.data()?.displayName || "Someone";

    await messaging.send({
      token,
      data: {
        type: "profile_viewed",
        title: "Profile Viewed 👀",
        body: `${viewerName} viewed your profile`,
        user_id: viewerUid,
        recipient_uid: viewedUid,
      },
      android: { priority: "normal", notification: { channelId: "match_system" } },
    });
  });
