import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db } from "./shared";
import { hasActiveConsent } from "./consent";

type FeedbackKind =
  | "PROFILE_OPEN"
  | "SHORTLIST"
  | "INTEREST_SENT"
  | "MUTUAL_MATCH"
  | "HIDDEN"
  | "REPORTED";

const FIELD_BY_KIND: Record<FeedbackKind, string> = {
  PROFILE_OPEN: "profileOpenCount",
  SHORTLIST: "shortlistCount",
  INTEREST_SENT: "interestSentCount",
  MUTUAL_MATCH: "mutualMatchCount",
  HIDDEN: "hiddenCount",
  REPORTED: "reportedCount",
};

const LAST_FIELD_BY_KIND: Record<FeedbackKind, string> = {
  PROFILE_OPEN: "lastProfileOpenAt",
  SHORTLIST: "lastShortlistAt",
  INTEREST_SENT: "lastInterestSentAt",
  MUTUAL_MATCH: "lastMutualMatchAt",
  HIDDEN: "lastHiddenAt",
  REPORTED: "lastReportedAt",
};

async function recordPairFeedback(
  viewerUid: string,
  targetUid: string,
  kind: FeedbackKind
): Promise<void> {
  if (!viewerUid || !targetUid || viewerUid === targetUid) return;
  if (!(await hasActiveConsent(viewerUid, "personalization"))) return;
  const ref = db.collection("recommendationFeedback")
    .doc(viewerUid)
    .collection("targets")
    .doc(targetUid);
  await ref.set({
    viewerUid,
    targetUid,
    [FIELD_BY_KIND[kind]]: admin.firestore.FieldValue.increment(1),
    [LAST_FIELD_BY_KIND[kind]]: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
}

/**
 * One discovery request creates one compact impression batch instead of one database write per
 * profile. The batch is suitable for later offline ranking analysis and carries a TTL field.
 */
export async function recordRecommendationImpressionBatch(
  viewerUid: string,
  targetUids: string[],
  source = "DISCOVER"
): Promise<void> {
  const unique = [...new Set(targetUids)]
    .filter((uid) => uid && uid !== viewerUid)
    .slice(0, 50);
  if (!viewerUid || unique.length === 0) return;
  if (!(await hasActiveConsent(viewerUid, "personalization"))) return;

  await db.collection("recommendationImpressionBatches").add({
    viewerUid,
    targetUids: unique,
    source,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    expireAt: admin.firestore.Timestamp.fromMillis(
      Date.now() + 90 * 24 * 60 * 60 * 1000
    ),
  });
}

export const onRecommendationProfileOpened = functions.firestore
  .document("profileViews/{viewId}")
  .onCreate(async (snap) => {
    const value = snap.data() || {};
    await recordPairFeedback(
      String(value.viewerUid || ""),
      String(value.viewedUid || ""),
      "PROFILE_OPEN"
    );
  });

export const onRecommendationShortlisted = functions.firestore
  .document("shortlists/{viewerUid}/saved/{targetUid}")
  .onCreate(async (_snap, context) => {
    await recordPairFeedback(
      context.params.viewerUid,
      context.params.targetUid,
      "SHORTLIST"
    );
  });

export const onRecommendationInterestSent = functions.firestore
  .document("interests/{interestId}")
  .onCreate(async (snap) => {
    const value = snap.data() || {};
    await recordPairFeedback(
      String(value.fromUid || ""),
      String(value.toUid || ""),
      "INTEREST_SENT"
    );
  });

export const onRecommendationMutualMatch = functions.firestore
  .document("matches/{matchId}")
  .onCreate(async (snap) => {
    const users = snap.data()?.users;
    if (!Array.isArray(users) || users.length !== 2) return;
    const uidA = typeof users[0] === "string" ? users[0] : "";
    const uidB = typeof users[1] === "string" ? users[1] : "";
    await Promise.all([
      recordPairFeedback(uidA, uidB, "MUTUAL_MATCH"),
      recordPairFeedback(uidB, uidA, "MUTUAL_MATCH"),
    ]);
  });

export const onRecommendationHidden = functions.firestore
  .document("privacyRelations/{viewerUid}/members/{targetUid}")
  .onWrite(async (change, context) => {
    const beforeHidden = change.before.data()?.profileHidden === true;
    const afterHidden = change.after.data()?.profileHidden === true;
    if (beforeHidden || !afterHidden) return;
    await recordPairFeedback(
      context.params.viewerUid,
      context.params.targetUid,
      "HIDDEN"
    );
  });

export const onRecommendationReported = functions.firestore
  .document("profileReports/{reportId}")
  .onCreate(async (snap) => {
    const value = snap.data() || {};
    await recordPairFeedback(
      String(value.reporterUid || ""),
      String(value.targetUid || ""),
      "REPORTED"
    );
  });
