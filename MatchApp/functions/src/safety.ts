import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import { db, requireAppCheck } from "./shared";

const REPORT_REASONS = new Set([
  "Fake profile",
  "Inappropriate content",
  "Harassment",
  "Spam or scam",
  "Under age",
  "Other",
]);

const MAX_UID_LENGTH = 128;

function dayKey(): string {
  return new Date().toISOString().slice(0, 10);
}

function stableId(...parts: string[]): string {
  return crypto.createHash("sha256").update(parts.join("|")).digest("hex");
}

function requireTargetUid(value: unknown, ownUid: string): string {
  const targetUid = typeof value === "string" ? value.trim() : "";
  if (!targetUid || targetUid === ownUid || targetUid.length > MAX_UID_LENGTH) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target profile");
  }
  return targetUid;
}

/**
 * Block is a trusted atomic safety action: create the block first-class state and remove both
 * interest directions plus any mutual-match record in the same transaction. Security rules make
 * the block immediately terminate profile/chat/media access even if a client still has stale UI.
 */
export const blockUser = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const blockerUid = context.auth?.uid;
  if (!blockerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const blockedUid = requireTargetUid(data?.targetUid, blockerUid);

  const targetRef = db.collection("users").doc(blockedUid);
  const blockRef = db.collection("blocks").doc(blockerUid).collection("blocked").doc(blockedUid);
  const outgoingRef = db.collection("interests").doc(`${blockerUid}_${blockedUid}`);
  const incomingRef = db.collection("interests").doc(`${blockedUid}_${blockerUid}`);
  const matchRef = db.collection("matches").doc([blockerUid, blockedUid].sort().join("_"));

  await db.runTransaction(async (tx) => {
    const target = await tx.get(targetRef);
    if (!target.exists) throw new functions.https.HttpsError("not-found", "Profile not found");

    tx.set(blockRef, {
      blockedUid,
      blockedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });
    tx.delete(outgoingRef);
    tx.delete(incomingRef);
    tx.delete(matchRef);
  });

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
  const reason = typeof data?.reason === "string" ? data.reason.trim() : "";
  const details = typeof data?.details === "string" ? data.details.trim().slice(0, 1000) : "";
  if (!targetUid || targetUid === reporterUid) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid profile to report");
  }
  if (!REPORT_REASONS.has(reason)) {
    throw new functions.https.HttpsError("invalid-argument", "Choose a valid report reason");
  }

  const target = await db.collection("users").doc(targetUid).get();
  if (!target.exists) throw new functions.https.HttpsError("not-found", "Profile not found");

  // One report per reporter/target/day prevents accidental repeated taps and basic spam while
  // still allowing a member to report new behaviour on a later date.
  const reportRef = db.collection("profileReports").doc(stableId(reporterUid, targetUid, dayKey()));
  const existing = await reportRef.get();
  if (existing.exists) return { success: true, alreadySubmitted: true };

  await reportRef.set({
    reporterUid,
    targetUid,
    reason,
    details: details || null,
    status: "OPEN",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  functions.logger.info("Profile report submitted", { reporterUid, targetUid, reason });
  return { success: true, alreadySubmitted: false };
});

const SUPPORT_CATEGORIES = new Set([
  "Account",
  "Membership",
  "Verification",
  "Safety",
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
  await ticketRef.set({
    uid,
    category,
    message,
    status: "OPEN",
    source: "ANDROID",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  functions.logger.info("Support ticket submitted", { uid, ticketId: ticketRef.id, category });
  return { success: true, ticketId: ticketRef.id };
});
