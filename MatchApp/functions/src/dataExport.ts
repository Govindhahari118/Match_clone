import * as admin from "firebase-admin";
import * as crypto from "crypto";
import * as functions from "firebase-functions/v1";
import { promises as fs } from "fs";
import * as os from "os";
import * as path from "path";
import { db, requireAppCheck } from "./shared";
import { DATA_EXPORT_COOLDOWN_MS, dataExportRetryAfterMs } from "./dataExportPolicy";

const EXPORT_LINK_MS = 10 * 60 * 1000;
const EXPORT_RETENTION_MS = 24 * 60 * 60 * 1000;
const PAGE_SIZE = 300;
const MAX_EXPORT_DOCS_PER_QUERY = 100_000;

function requireRecentAuth(context: functions.https.CallableContext): string {
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const authTimeSeconds = Number(context.auth?.token?.auth_time || 0);
  const ageMs = Date.now() - authTimeSeconds * 1000;
  if (!Number.isFinite(ageMs) || authTimeSeconds <= 0 || ageMs > 5 * 60 * 1000) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Recent authentication is required before exporting account data"
    );
  }
  return uid;
}

function normalize(value: unknown): unknown {
  if (value instanceof admin.firestore.Timestamp) return value.toDate().toISOString();
  if (value instanceof admin.firestore.GeoPoint) {
    return { latitude: value.latitude, longitude: value.longitude };
  }
  if (value instanceof admin.firestore.DocumentReference) return value.path;
  if (Array.isArray(value)) return value.map(normalize);
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>).map(([key, item]) => [key, normalize(item)])
    );
  }
  return value;
}

async function queryAll(query: FirebaseFirestore.Query): Promise<Array<Record<string, unknown>>> {
  const rows: Array<Record<string, unknown>> = [];
  let cursor: FirebaseFirestore.QueryDocumentSnapshot | undefined;
  while (rows.length < MAX_EXPORT_DOCS_PER_QUERY) {
    let page = query.orderBy(admin.firestore.FieldPath.documentId()).limit(PAGE_SIZE);
    if (cursor) page = page.startAfter(cursor);
    const snapshot = await page.get();
    if (snapshot.empty) break;
    snapshot.docs.forEach((doc) => rows.push({
      id: doc.id,
      ...(normalize(doc.data()) as Record<string, unknown>),
    }));
    cursor = snapshot.docs[snapshot.docs.length - 1];
    if (snapshot.size < PAGE_SIZE) break;
  }
  if (rows.length >= MAX_EXPORT_DOCS_PER_QUERY) {
    throw new functions.https.HttpsError(
      "resource-exhausted",
      "This account export is too large to generate safely in one request"
    );
  }
  return rows;
}

async function childCollection(pathValue: string): Promise<Array<Record<string, unknown>>> {
  return queryAll(db.collection(pathValue));
}

async function singleDoc(collection: string, uid: string): Promise<Record<string, unknown> | null> {
  const snap = await db.collection(collection).doc(uid).get();
  return snap.exists
    ? { id: snap.id, ...(normalize(snap.data() || {}) as Record<string, unknown>) }
    : null;
}

function paymentExport(payment: Record<string, unknown>): Record<string, unknown> {
  const {
    tokenHash: _tokenHash,
    purchaseTokenHash: _purchaseTokenHash,
    ...safe
  } = payment;
  return safe;
}

function verificationExport(
  verification: Record<string, unknown> | null
): Record<string, unknown> | null {
  if (!verification) return null;
  const {
    documentPath: _documentPath,
    reviewedBy: _reviewedBy,
    ...safe
  } = verification;
  return safe;
}

function submittedReportExport(report: Record<string, unknown>): Record<string, unknown> {
  const {
    lastReviewedBy: _lastReviewedBy,
    lastReviewedRole: _lastReviewedRole,
    resolutionReason: _resolutionReason,
    ...safe
  } = report;
  return safe;
}

function mediaModerationExport(row: Record<string, unknown>): Record<string, unknown> {
  const {
    reviewedBy: _reviewedBy,
    reviewedRole: _reviewedRole,
    reviewReason: _reviewReason,
    duplicateAcrossAccounts: _duplicateAcrossAccounts,
    ...safe
  } = row;
  return safe;
}

async function buildExport(uid: string): Promise<Record<string, unknown>> {
  const [
    profile,
    privateProfile,
    partnerPreferences,
    privacySettings,
    notificationPrefs,
    appearancePrefs,
    subscription,
    payments,
    verification,
    verificationRequest,
    location,
    consents,
    consentHistory,
    savedSearches,
    shortlist,
    blocks,
    privacyRelations,
    chatPreferences,
    contactGrants,
    notifications,
    outgoingInterests,
    incomingInterests,
    matches,
    profileViewsByMe,
    profileViewsOfMe,
    recommendationFeedback,
    recommendationImpressions,
    profileAnalytics,
    submittedReports,
    photoModeration,
    videoModeration,
    supportTickets,
    assistedRequest,
    familyDelegates,
    familyManagedProfiles,
  ] = await Promise.all([
    singleDoc("users", uid),
    singleDoc("userPrivate", uid),
    singleDoc("partnerPreferences", uid),
    singleDoc("privacySettings", uid),
    singleDoc("notificationPrefs", uid),
    singleDoc("appearancePrefs", uid),
    singleDoc("subscriptions", uid),
    queryAll(db.collection("payments").where("uid", "==", uid)),
    singleDoc("verifications", uid),
    singleDoc("verificationRequests", uid),
    singleDoc("userLocations", uid),
    childCollection(`consents/${uid}/items`),
    childCollection(`consentLedger/${uid}/events`),
    childCollection(`savedSearches/${uid}/items`),
    childCollection(`shortlists/${uid}/saved`),
    childCollection(`blocks/${uid}/blocked`),
    childCollection(`privacyRelations/${uid}/members`),
    childCollection(`chatPreferences/${uid}/threads`),
    childCollection(`contactGrants/${uid}/viewers`),
    queryAll(db.collection("notifications").where("userId", "==", uid)),
    queryAll(db.collection("interests").where("fromUid", "==", uid)),
    queryAll(db.collection("interests").where("toUid", "==", uid)),
    queryAll(db.collection("matches").where("users", "array-contains", uid)),
    queryAll(db.collection("profileViews").where("viewerUid", "==", uid)),
    queryAll(db.collection("profileViews").where("viewedUid", "==", uid)),
    childCollection(`recommendationFeedback/${uid}/targets`),
    queryAll(db.collection("recommendationImpressionBatches").where("viewerUid", "==", uid)),
    childCollection(`profileAnalytics/${uid}/weekly`),
    queryAll(db.collection("profileReports").where("reporterUid", "==", uid)),
    queryAll(db.collection("photoModeration").where("uid", "==", uid)),
    queryAll(db.collection("videoModeration").where("uid", "==", uid)),
    queryAll(db.collection("supportTickets").where("uid", "==", uid)),
    singleDoc("rmRequests", uid),
    childCollection(`familyDelegates/${uid}/members`),
    queryAll(db.collectionGroup("members").where("delegateUid", "==", uid)),
  ]);

  const chatSnapshots = await queryAll(
    db.collection("chats").where("participantUids", "array-contains", uid)
  );
  const chats: Array<Record<string, unknown>> = [];
  for (const chat of chatSnapshots) {
    const threadId = String(chat.id || "");
    const messages = await childCollection(`chats/${threadId}/messages`);
    chats.push({ ...chat, messages });
  }

  return {
    schemaVersion: 3,
    generatedAt: new Date().toISOString(),
    accountUid: uid,
    scope: {
      includes: [
        "profile",
        "private profile",
        "preferences and privacy settings",
        "subscription and payment history with provider tokens removed",
        "consent state and history",
        "saved searches and shortlist",
        "blocks and per-member privacy choices",
        "chat mute and archive preferences",
        "notifications",
        "interests and matches",
        "profile-view history involving this account",
        "consented recommendation feedback and impression history",
        "derived profile analytics",
        "reports submitted by this account",
        "profile photo/video moderation status with internal review data removed",
        "chat threads and messages",
        "support tickets and assisted-matchmaking requests",
        "family profile delegation relationships",
        "verification status",
        "current stored Nearby location, if any",
      ],
      excludes: [
        "raw KYC files",
        "internal fraud/risk signals, including duplicate-photo risk evidence",
        "operator identities, internal review reasons and audit records",
        "payment-provider secrets or tokens",
      ],
    },
    data: {
      profile,
      privateProfile,
      partnerPreferences,
      privacySettings,
      notificationPrefs,
      appearancePrefs,
      subscription,
      payments: payments.map(paymentExport),
      verification: verificationExport(verification),
      verificationRequest: verificationExport(verificationRequest),
      location,
      consents,
      consentHistory,
      savedSearches,
      shortlist,
      blocks,
      privacyRelations,
      chatPreferences,
      contactGrants,
      notifications,
      interests: {
        sent: outgoingInterests,
        received: incomingInterests,
      },
      matches,
      profileViews: {
        viewedByMe: profileViewsByMe,
        viewedMe: profileViewsOfMe,
      },
      recommendation: {
        feedback: recommendationFeedback,
        impressions: recommendationImpressions,
      },
      profileAnalytics,
      submittedReports: submittedReports.map(submittedReportExport),
      mediaModeration: {
        photos: photoModeration.map(mediaModerationExport),
        videos: videoModeration.map(mediaModerationExport),
      },
      chats,
      supportTickets,
      assistedMatchmakingRequest: assistedRequest,
      familyAccess: {
        delegates: familyDelegates,
        managedProfiles: familyManagedProfiles.filter((item) => item.active === true),
      },
    },
  };
}

export const createMyDataExport = functions
  .runWith({ timeoutSeconds: 540, memory: "1GB" })
  .https.onCall(async (_data, context) => {
    requireAppCheck(context);
    const uid = requireRecentAuth(context);
    const now = Date.now();
    const throttleRef = db.collection("dataExportRateLimits").doc(uid);
    await db.runTransaction(async (tx) => {
      const throttle = await tx.get(throttleRef);
      const retryAfterMillis = dataExportRetryAfterMs(
        throttle.data()?.nextAllowedAtMillis,
        now
      );
      if (throttle.exists && retryAfterMillis > 0) {
        throw new functions.https.HttpsError(
          "resource-exhausted",
          "A recent export request is still within the safety cooldown. Try again later.",
          { retryAfterMillis }
        );
      }
      tx.set(throttleRef, {
        uid,
        lastRequestedAt: admin.firestore.FieldValue.serverTimestamp(),
        nextAllowedAtMillis: now + DATA_EXPORT_COOLDOWN_MS,
      }, { merge: false });
    });

    const requestId = crypto.randomUUID();
    const storagePath = `exports/${uid}/${requestId}.json`;
    const tempPath = path.join(os.tmpdir(), `matree-export-${requestId}.json`);
    const expiresAt = admin.firestore.Timestamp.fromMillis(Date.now() + EXPORT_RETENTION_MS);

    try {
      const payload = await buildExport(uid);
      await fs.writeFile(tempPath, JSON.stringify(payload, null, 2), { encoding: "utf8", mode: 0o600 });

      const file = admin.storage().bucket().file(storagePath);
      await admin.storage().bucket().upload(tempPath, {
        destination: storagePath,
        resumable: false,
        metadata: {
          contentType: "application/json",
          cacheControl: "private, no-store, max-age=0",
          contentDisposition: "attachment; filename=\"matree-account-data.json\"",
          metadata: {
            ownerUid: uid,
            exportRequestId: requestId,
          },
        },
      });

      await db.collection("dataExportRequests").doc(requestId).set({
        uid,
        storagePath,
        status: "READY",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        expireAt: expiresAt,
      });

      const linkExpiresAtMillis = Date.now() + EXPORT_LINK_MS;
      const [downloadUrl] = await file.getSignedUrl({
        action: "read",
        expires: linkExpiresAtMillis,
      });

      return {
        requestId,
        downloadUrl,
        linkExpiresAtMillis,
        exportExpiresAtMillis: expiresAt.toMillis(),
      };
    } catch (error) {
      functions.logger.error("Account data export failed", {
        uid,
        requestId,
        error: error instanceof Error ? error.message : String(error),
      });
      throw error instanceof functions.https.HttpsError
        ? error
        : new functions.https.HttpsError("internal", "Account data export could not be generated");
    } finally {
      await fs.unlink(tempPath).catch(() => undefined);
    }
  });

export const cleanupExpiredDataExports = functions.pubsub
  .schedule("every 24 hours")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const expired = await db.collection("dataExportRequests")
      .where("expireAt", "<=", admin.firestore.Timestamp.now())
      .limit(200)
      .get();

    for (const doc of expired.docs) {
      const value = doc.data() || {};
      const uid = String(value.uid || "");
      const storagePath = String(value.storagePath || "");
      if (uid && storagePath.startsWith(`exports/${uid}/`) && !storagePath.includes("..")) {
        await admin.storage().bucket().file(storagePath).delete({ ignoreNotFound: true });
      }
      await doc.ref.delete();
    }
  });
