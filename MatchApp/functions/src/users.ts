import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { db, messaging, generateMatrimonyId } from "./shared";

// ── User Created → Generate matrimonyId + init analytics ───────────────────
export const onUserCreate = functions.firestore
  .document("users/{uid}")
  .onCreate(async (snap, context) => {
    const uid = context.params.uid;
    const matrimonyId = generateMatrimonyId();

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

// ── Inactivity Nudge (Scheduled: daily check) ─────────────────────────────
export const sendInactivityNudge = functions.pubsub
  .schedule("0 5 * * *")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = Date.now();
    const sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000;
    const fourteenDaysAgo = now - 14 * 24 * 60 * 60 * 1000;

    const nudge7 = await db
      .collection("users")
      .where("lastActiveAt", "<=", sevenDaysAgo)
      .where("lastActiveAt", ">", fourteenDaysAgo)
      .limit(100)
      .get();

    const sends: Promise<string>[] = [];
    for (const doc of nudge7.docs) {
      const fcmToken = doc.data().fcmToken as string | undefined;
      if (!fcmToken) continue;

      sends.push(
        messaging.send({
          token: fcmToken,
          data: {
            type: "inactivity_nudge",
            title: "We miss you! 💝",
            body: "New profiles matching your preferences are waiting. Come back and explore!",
          },
          android: {
            priority: "normal",
            notification: { channelId: "match_system" },
          },
        })
      );
    }

    await Promise.allSettled(sends);
    functions.logger.info(`Inactivity nudges sent: ${sends.length}`);
  });

// ── Profile Incomplete Reminders (Day 2, Day 7) ──────────────────────────
export const sendProfileIncompleteD2 = functions.pubsub
  .schedule("0 4 * * *")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = Date.now();
    const twoDaysAgo = now - 2 * 24 * 60 * 60 * 1000;
    const threeDaysAgo = now - 3 * 24 * 60 * 60 * 1000;

    const incomplete = await db
      .collection("users")
      .where("profileCompleteness", "<", 0.5)
      .where("createdAt", "<=", twoDaysAgo)
      .where("createdAt", ">", threeDaysAgo)
      .limit(200)
      .get();

    const sends: Promise<string>[] = [];
    for (const doc of incomplete.docs) {
      const fcmToken = doc.data().fcmToken as string | undefined;
      if (!fcmToken) continue;
      sends.push(
        messaging.send({
          token: fcmToken,
          data: {
            type: "profile_incomplete",
            title: "Complete your profile 📝",
            body: "Profiles with 80%+ completeness get 5x more views. Finish yours now!",
          },
          android: { priority: "normal", notification: { channelId: "match_system" } },
        })
      );
    }
    await Promise.allSettled(sends);
  });

export const sendProfileIncompleteD7 = functions.pubsub
  .schedule("0 4 * * *")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = Date.now();
    const sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000;
    const eightDaysAgo = now - 8 * 24 * 60 * 60 * 1000;

    const incomplete = await db
      .collection("users")
      .where("profileCompleteness", "<", 0.6)
      .where("createdAt", "<=", sevenDaysAgo)
      .where("createdAt", ">", eightDaysAgo)
      .limit(200)
      .get();

    const sends: Promise<string>[] = [];
    for (const doc of incomplete.docs) {
      const fcmToken = doc.data().fcmToken as string | undefined;
      if (!fcmToken) continue;
      sends.push(
        messaging.send({
          token: fcmToken,
          data: {
            type: "profile_incomplete",
            title: "Your profile is 60% done 💡",
            body: "Add your horoscope details and photo to attract better matches.",
          },
          android: { priority: "normal", notification: { channelId: "match_system" } },
        })
      );
    }
    await Promise.allSettled(sends);
  });

// ── Cascading Account Deletion ────────────────────────────────────────────
export const deleteUserAccount = functions
  .runWith({ timeoutSeconds: 300, memory: "512MB" })
  .https.onCall(async (_data, context) => {
    const uid = context.auth?.uid;
    if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

    const batch = db.batch();
    const ops: Promise<unknown>[] = [];

    batch.delete(db.collection("users").doc(uid));

    ops.push(
      db.collection("interests").where("fromUid", "==", uid).get().then((s) => s.docs.forEach((d) => batch.delete(d.ref))),
      db.collection("interests").where("toUid", "==", uid).get().then((s) => s.docs.forEach((d) => batch.delete(d.ref))),
      db.collection("matches").where("users", "array-contains", uid).get().then((s) => s.docs.forEach((d) => batch.delete(d.ref))),
      db.collection("shortlists").doc(uid).collection("saved").get().then((s) => {
        s.docs.forEach((d) => batch.delete(d.ref));
        batch.delete(db.collection("shortlists").doc(uid));
      }),
      db.collection("blocks").doc(uid).collection("blocked").get().then((s) => {
        s.docs.forEach((d) => batch.delete(d.ref));
        batch.delete(db.collection("blocks").doc(uid));
      }),
      db.collection("subscriptions").doc(uid).collection("usage").get().then((s) => {
        s.docs.forEach((d) => batch.delete(d.ref));
        batch.delete(db.collection("subscriptions").doc(uid));
      })
    );

    batch.delete(db.collection("fcmTokens").doc(uid));

    await Promise.all(ops);
    await batch.commit();

    try { await admin.auth().deleteUser(uid); } catch (err) { functions.logger.warn("Auth deleteUser failed", err); }

    await db.collection("deletionAudit").add({
      uid,
      deletedAt: admin.firestore.FieldValue.serverTimestamp(),
      expireAt: admin.firestore.Timestamp.fromMillis(Date.now() + 30 * 24 * 60 * 60 * 1000),
    });

    return { success: true };
  });

// ── Profile View Tracking ──────────────────────────────────────────────────
export const onProfileViewed = functions.firestore
  .document("profileViews/{viewId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    const viewedUid = data.viewedUid as string;
    const viewerUid = data.viewerUid as string;

    await db.collection("users").doc(viewedUid).update({
      profileViewCount: admin.firestore.FieldValue.increment(1),
    });

    const viewedDoc = await db.collection("users").doc(viewedUid).get();
    const fcmToken = viewedDoc.data()?.fcmToken as string | undefined;
    if (!fcmToken) return;

    const viewerDoc = await db.collection("users").doc(viewerUid).get();
    const viewerName = viewerDoc.data()?.displayName || "Someone";

    await messaging.send({
      token: fcmToken,
      data: {
        type: "profile_viewed",
        title: "Profile Viewed 👀",
        body: `${viewerName} viewed your profile`,
        user_id: viewerUid,
      },
      android: { priority: "normal", notification: { channelId: "match_system" } },
    });
  });
