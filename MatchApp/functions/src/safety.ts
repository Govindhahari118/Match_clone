import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import { db, requireAppCheck } from "./shared";
import {
  MAX_DAILY_REPORTS,
  MAX_DAILY_SUPPORT_TICKETS,
  safeUsageCount,
  usageAllowed,
} from "./abusePolicy";
import { normalizeReportReason } from "./reportReasonPolicy";
import { canonicalChatThreadId } from "./chatIdentityPolicy";

const MAX_UID_LENGTH = 128;

function dayKey(): string {
  return new Date().toISOString().slice(0, 10);
}

function stableId(...parts: string[]): string {
  return crypto.createHash("sha256").update(parts.join("|")).digest("hex");
}

function pairId(uidA: string, uidB: string): string {
  return [uidA, uidB].sort().join("_");
}

function requireTargetUid(value: unknown, ownUid: string): string {
  const targetUid = typeof value === "string" ? value.trim() : "";
  if (!targetUid || targetUid === ownUid || targetUid.length > MAX_UID_LENGTH) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target profile");
  }
  return targetUid;
}

/**
 * Create the block and remove all direct relationship state in one trusted transaction. Chat
 * records are retained for moderation/account history; Firestore/Storage rules revoke access as
 * soon as the block exists.
 */
export const blockUser = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const blockerUid = context.auth?.uid;
  if (!blockerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const blockedUid = requireTargetUid(data?.targetUid, blockerUid);

  const targetRef = db.collection("users").doc(blockedUid);
  const blockRef = db.collection("blocks").doc(blockerUid).collection("blocked").doc(blockedUid);

  await db.runTransaction(async (tx) => {
    const target = await tx.get(targetRef);
    if (!target.exists) throw new functions.https.HttpsError("not-found", "Profile not found");

    tx.set(blockRef, {
      blockedUid,
      blockedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });
    tx.delete(db.collection("interests").doc(`${blockerUid}_${blockedUid}`));
    tx.delete(db.collection("interests").doc(`${blockedUid}_${blockerUid}`));
    tx.delete(db.collection("interestResponses").doc(`${blockerUid}_${blockedUid}`));
    tx.delete(db.collection("interestResponses").doc(`${blockedUid}_${blockerUid}`));
    tx.delete(db.collection("matches").doc(pairId(blockerUid, blockedUid)));
    tx.delete(db.collection("callRequests").doc(pairId(blockerUid, blockedUid)));
    tx.delete(db.collection("shortlists").doc(blockerUid).collection("saved").doc(blockedUid));
    tx.delete(db.collection("shortlists").doc(blockedUid).collection("saved").doc(blockerUid));
    tx.delete(db.collection("contactGrants").doc(blockerUid).collection("viewers").doc(blockedUid));
    tx.delete(db.collection("contactGrants").doc(blockedUid).collection("viewers").doc(blockerUid));
    tx.delete(db.collection("contactRequests").doc(`${blockerUid}_${blockedUid}`));
    tx.delete(db.collection("contactRequests").doc(`${blockedUid}_${blockerUid}`));
    tx.delete(db.collection("photoGrants").doc(blockerUid).collection("viewers").doc(blockedUid));
    tx.delete(db.collection("photoGrants").doc(blockedUid).collection("viewers").doc(blockerUid));
    tx.delete(db.collection("photoRequests").doc(`${blockerUid}_${blockedUid}`));
    tx.delete(db.collection("photoAccessRequests").doc(`${blockerUid}_${blockedUid}`));
    tx.delete(db.collection("photoRequests").doc(`${blockedUid}_${blockerUid}`));
    tx.delete(db.collection("photoAccessRequests").doc(`${blockedUid}_${blockerUid}`));
  });

  functions.logger.info("Member blocked and relationship state removed", { blockerUid, blockedUid });
  return { success: true };
});

export const unblockUser = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const blockerUid = context.auth?.uid;
  if (!blockerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const blockedUid = requireTargetUid(data?.targetUid, blockerUid);

  await db.collection("blocks").doc(blockerUid).collection("blocked").doc(blockedUid).delete();
  return { success: true };
});

export const submitProfileReport = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const reporterUid = context.auth?.uid;
  if (!reporterUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = typeof data?.targetUid === "string" ? data.targetUid.trim() : "";
  const reason = normalizeReportReason(data?.reason);
  const details = typeof data?.details === "string" ? data.details.trim().slice(0, 1000) : "";
  if (!targetUid || targetUid === reporterUid) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid profile to report");
  }
  if (!reason) {
    throw new functions.https.HttpsError("invalid-argument", "Choose a valid report reason");
  }

  const targetRef = db.collection("users").doc(targetUid);
  const day = dayKey();
  const reportRef = db.collection("profileReports").doc(stableId(reporterUid, targetUid, day));
  const activityRef = db.collection("riskActivity").doc(reporterUid).collection("days").doc(day);
  const created = await db.runTransaction(async (tx) => {
    const [target, existing, activity] = await Promise.all([
      tx.get(targetRef),
      tx.get(reportRef),
      tx.get(activityRef),
    ]);
    if (!target.exists) {
      throw new functions.https.HttpsError("not-found", "Profile not found");
    }
    if (existing.exists) return false;

    const reportCount = safeUsageCount(activity.data()?.reportCount);
    if (!usageAllowed(reportCount, MAX_DAILY_REPORTS)) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "Daily report safety limit reached. Use Block for immediate protection or contact support."
      );
    }

    tx.create(reportRef, {
      reporterUid,
      targetUid,
      reason,
      details: details || null,
      status: "OPEN",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.set(activityRef, {
      reportCount: reportCount + 1,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    return true;
  });

  if (created) {
    functions.logger.info("Profile report submitted", { reporterUid, targetUid, reason });
  }
  return { success: true, alreadySubmitted: !created };
});


/**
 * Report a specific message while preserving immutable server-side moderation evidence.
 *
 * The reporter must be the recipient of the reported message and a participant in the canonical
 * thread. Client-provided message text/media is never trusted; the evidence snapshot is copied
 * from the server-owned message document.
 */
export const submitChatMessageReport = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const reporterUid = context.auth?.uid;
  if (!reporterUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = requireTargetUid(data?.targetUid, reporterUid);
  const messageId = typeof data?.messageId === "string" ? data.messageId.trim() : "";
  const reason = normalizeReportReason(data?.reason);
  const details = typeof data?.details === "string" ? data.details.trim().slice(0, 1000) : "";
  if (!messageId || !/^[A-Za-z0-9_-]{16,128}$/.test(messageId)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid message to report");
  }
  if (!reason) {
    throw new functions.https.HttpsError("invalid-argument", "Choose a valid report reason");
  }

  const threadId = canonicalChatThreadId(reporterUid, targetUid);
  const threadRef = db.collection("chats").doc(threadId);
  const messageRef = threadRef.collection("messages").doc(messageId);
  const day = dayKey();
  const reportRef = db.collection("chatMessageReports")
    .doc(stableId(reporterUid, threadRef.id, messageId));
  const activityRef = db.collection("riskActivity").doc(reporterUid).collection("days").doc(day);

  const created = await db.runTransaction(async (tx) => {
    const [threadSnap, messageSnap, existing, activity] = await Promise.all([
      tx.get(threadRef),
      tx.get(messageRef),
      tx.get(reportRef),
      tx.get(activityRef),
    ]);
    if (!threadSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Conversation not found");
    }
    const participants = Array.isArray(threadSnap.data()?.participantUids) ?
      threadSnap.data()?.participantUids : [];
    if (participants.length !== 2 ||
        !participants.includes(reporterUid) ||
        !participants.includes(targetUid)) {
      throw new functions.https.HttpsError("permission-denied", "Conversation unavailable");
    }
    if (!messageSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Message not found");
    }
    const message = messageSnap.data() || {};
    if (message.fromFirebaseUid !== targetUid || message.toFirebaseUid !== reporterUid) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Only received messages can be reported from this conversation"
      );
    }
    if (existing.exists) return false;

    const reportCount = safeUsageCount(activity.data()?.reportCount);
    if (!usageAllowed(reportCount, MAX_DAILY_REPORTS)) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "Daily report safety limit reached. Use Block for immediate protection or contact support."
      );
    }

    tx.create(reportRef, {
      reporterUid,
      targetUid,
      threadId: threadRef.id,
      messageId,
      reason,
      details: details || null,
      status: "OPEN",
      evidence: {
        fromFirebaseUid: String(message.fromFirebaseUid || ""),
        toFirebaseUid: String(message.toFirebaseUid || ""),
        body: typeof message.body === "string" ? message.body.slice(0, 3000) : "",
        imageUri: typeof message.imageUri === "string" ? message.imageUri : null,
        voiceUri: typeof message.voiceUri === "string" ? message.voiceUri : null,
        voiceDurationMs: Number(message.voiceDurationMs || 0),
        sentAt: Number(message.sentAt || 0),
      },
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.set(activityRef, {
      reportCount: reportCount + 1,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    return true;
  });

  if (created) {
    functions.logger.info("Chat message report submitted", {
      reporterUid,
      targetUid,
      threadId: threadRef.id,
      messageId,
      reason,
    });
  }
  return { success: true, alreadySubmitted: !created };
});

const SUPPORT_CATEGORIES = new Set([
  "Account",
  "Verification",
  "Match/search",
  "Membership",
  "Payment",
  "Privacy",
  "Safety",
  "Appeal",
  "Technical issue",
  "Other",
]);

export const submitSupportTicket = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const category = typeof data?.category === "string" ? data.category.trim() : "";
  const message = typeof data?.message === "string" ? data.message.trim() : "";
  if (!SUPPORT_CATEGORIES.has(category)) {
    throw new functions.https.HttpsError("invalid-argument", "Choose a valid support category");
  }
  if (message.length < 10 || message.length > 2000) {
    throw new functions.https.HttpsError("invalid-argument", "Message must be 10-2000 characters");
  }

  const ticketRef = db.collection("supportTickets").doc();
  const activityRef = db.collection("riskActivity").doc(uid).collection("days").doc(dayKey());
  await db.runTransaction(async (tx) => {
    const activity = await tx.get(activityRef);
    const supportTicketCount = safeUsageCount(activity.data()?.supportTicketCount);
    if (!usageAllowed(supportTicketCount, MAX_DAILY_SUPPORT_TICKETS)) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "Daily support request limit reached. Please continue an existing ticket where possible."
      );
    }

    tx.create(ticketRef, {
      uid,
      category,
      message,
      status: "OPEN",
      source: "ANDROID",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.set(activityRef, {
      supportTicketCount: supportTicketCount + 1,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  functions.logger.info("Support ticket submitted", { uid, ticketId: ticketRef.id, category });
  return { success: true, ticketId: ticketRef.id };
});


function supportTimestampMillis(value: unknown): number | null {
  return value instanceof admin.firestore.Timestamp ? value.toMillis() : null;
}

/**
 * Owner-only support history. Internal assignment, operator notes and escalation details stay in
 * the role-gated operations API.
 */
export const listMySupportTickets = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const snapshot = await db.collection("supportTickets")
    .where("uid", "==", uid)
    .limit(50)
    .get();

  const tickets = snapshot.docs.map((doc) => {
    const value = doc.data();
    return {
      id: doc.id,
      category: String(value.category || ""),
      message: String(value.message || ""),
      status: String(value.status || "OPEN"),
      createdAtMillis: supportTimestampMillis(value.createdAt),
      updatedAtMillis: supportTimestampMillis(value.updatedAt),
      resolvedAtMillis: supportTimestampMillis(value.resolvedAt),
    };
  }).sort((a, b) => (b.createdAtMillis || 0) - (a.createdAtMillis || 0));

  return { tickets };
});
