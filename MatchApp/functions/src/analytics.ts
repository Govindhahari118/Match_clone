import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

/**
 * Returns only counts derived from real relationship documents owned by or referring to the
 * authenticated account. No synthetic ranking, demographic inference or benchmark percentages
 * are generated here.
 */
export const getProfileAnalytics = functions
  .runWith({ timeoutSeconds: 30, memory: "256MB" })
  .https.onCall(async (_data, context) => {
    requireAppCheck(context);
    const uid = context.auth?.uid;
    if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

    const userRef = db.collection("users").doc(uid);
    const [
      user,
      views,
      received,
      sent,
      matches,
      conversations,
      shortlistedBy,
    ] = await Promise.all([
      userRef.get(),
      db.collection("profileViews").where("viewedUid", "==", uid).count().get(),
      db.collection("interests").where("toUid", "==", uid).count().get(),
      db.collection("interests").where("fromUid", "==", uid).count().get(),
      db.collection("matches").where("users", "array-contains", uid).count().get(),
      db.collection("chats").where("participantUids", "array-contains", uid).count().get(),
      db.collectionGroup("saved").where("targetUid", "==", uid).count().get(),
    ]);

    if (!user.exists) {
      throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
    }

    return {
      profileViews: views.data().count,
      interestsReceived: received.data().count,
      interestsSent: sent.data().count,
      mutualMatches: matches.data().count,
      conversations: conversations.data().count,
      shortlistedBy: shortlistedBy.data().count,
      isVerified: user.data()?.isVerified === true,
    };
  });
