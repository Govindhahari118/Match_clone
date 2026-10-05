import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { accountIsActive } from "./accountStatusPolicy";
import { db, requireAppCheck } from "./shared";
import {
  photoRequestCoolingDown,
  photoRequestNextAllowedAt,
} from "./photoRequestPolicy";

function cleanTargetUid(value: unknown): string {
  const uid = typeof value === "string" ? value.trim() : "";
  if (!uid || uid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "A valid target member is required");
  }
  return uid;
}

async function relationshipAvailable(requesterUid: string, targetUid: string) {
  const [
    requester,
    target,
    outgoingBlock,
    incomingBlock,
    requesterPrivacy,
    targetPrivacy,
  ] = await Promise.all([
    db.collection("users").doc(requesterUid).get(),
    db.collection("users").doc(targetUid).get(),
    db.collection("blocks").doc(requesterUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(requesterUid).get(),
    db.collection("privacyRelations").doc(requesterUid).collection("members").doc(targetUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(requesterUid).get(),
  ]);

  if (!requester.exists || !target.exists ||
      !accountIsActive(requester.data()?.accountStatus) ||
      !accountIsActive(target.data()?.accountStatus)) {
    throw new functions.https.HttpsError("failed-precondition", "Profile is unavailable");
  }
  if (outgoingBlock.exists || incomingBlock.exists ||
      requesterPrivacy.data()?.profileHidden === true ||
      targetPrivacy.data()?.profileHidden === true) {
    throw new functions.https.HttpsError("permission-denied", "Profile is unavailable");
  }

  return { target };
}

export const getProfilePhotoRequestStatus = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const requesterUid = context.auth?.uid;
  if (!requesterUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = cleanTargetUid(data?.targetUid);
  if (targetUid === requesterUid) {
    throw new functions.https.HttpsError("invalid-argument", "Cannot request your own photo");
  }

  const { target } = await relationshipAvailable(requesterUid, targetUid);
  if (String(target.data()?.photoUrl || "").trim()) {
    return { canRequest: false, reason: "PHOTO_AVAILABLE", nextAllowedAtMillis: 0 };
  }

  const requestId = `${requesterUid}_${targetUid}`;
  const existing = await db.collection("photoRequests").doc(requestId).get();
  const lastRequestedAtMillis = Number(existing.data()?.requestedAtMillis || 0);
  const now = Date.now();
  const coolingDown = photoRequestCoolingDown(lastRequestedAtMillis, now);

  return {
    canRequest: !coolingDown,
    reason: coolingDown ? "COOLDOWN" : "AVAILABLE",
    nextAllowedAtMillis: coolingDown ? photoRequestNextAllowedAt(lastRequestedAtMillis) : 0,
  };
});

export const requestProfilePhoto = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const requesterUid = context.auth?.uid;
  if (!requesterUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = cleanTargetUid(data?.targetUid);
  if (targetUid === requesterUid) {
    throw new functions.https.HttpsError("invalid-argument", "Cannot request your own photo");
  }

  const { target } = await relationshipAvailable(requesterUid, targetUid);
  if (String(target.data()?.photoUrl || "").trim()) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "This profile already has a published photo"
    );
  }

  const requestRef = db.collection("photoRequests").doc(`${requesterUid}_${targetUid}`);
  const now = Date.now();
  let created = false;
  let nextAllowedAtMillis = 0;

  await db.runTransaction(async (tx) => {
    const current = await tx.get(requestRef);
    const lastRequestedAtMillis = Number(current.data()?.requestedAtMillis || 0);
    if (photoRequestCoolingDown(lastRequestedAtMillis, now)) {
      nextAllowedAtMillis = photoRequestNextAllowedAt(lastRequestedAtMillis);
      return;
    }

    const sequence = Number(current.data()?.requestSequence || 0) + 1;
    tx.set(requestRef, {
      requesterUid,
      targetUid,
      status: "PENDING",
      requestSequence: sequence,
      requestedAtMillis: now,
      requestedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });
    created = true;
    nextAllowedAtMillis = now + 7 * 24 * 60 * 60 * 1000;
  });

  return {
    requested: created,
    coolingDown: !created,
    nextAllowedAtMillis,
  };
});
