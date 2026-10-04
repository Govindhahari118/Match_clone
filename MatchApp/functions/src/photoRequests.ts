import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import {
  db,
  persistAndSendNotification,
  requireAppCheck,
} from "./shared";
import { accountIsActive } from "./accountStatusPolicy";
import { notificationActionFor, notificationDeepLink } from "./notificationLinkPolicy";

const PHOTO_REQUEST_COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000;
const PHOTO_REQUEST_DAILY_LIMIT = 20;

function cleanUid(value: unknown): string {
  const uid = typeof value === "string" ? value.trim() : "";
  if (!uid || uid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "A valid target member is required");
  }
  return uid;
}

function requestId(requesterUid: string, targetUid: string): string {
  return `${requesterUid}_${targetUid}`;
}

function dayKey(now: number): string {
  return new Date(now).toISOString().slice(0, 10);
}

function publicPhotoAvailable(data: FirebaseFirestore.DocumentData | undefined): boolean {
  return typeof data?.photoUrl === "string" && data.photoUrl.trim().length > 0;
}

/**
 * Returns the requester's current server-owned photo-request state for one profile.
 */
export const getPhotoRequestStatus = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const requesterUid = context.auth?.uid;
  if (!requesterUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = cleanUid(data?.targetUid);
  if (targetUid === requesterUid) {
    throw new functions.https.HttpsError("invalid-argument", "Cannot request your own photo");
  }

  const [target, request] = await Promise.all([
    db.collection("users").doc(targetUid).get(),
    db.collection("photoRequests").doc(requestId(requesterUid, targetUid)).get(),
  ]);

  if (!target.exists || !accountIsActive(target.data()?.accountStatus)) {
    return { status: "UNAVAILABLE", requestedAtMillis: 0, retryAfterMillis: 0 };
  }
  if (publicPhotoAvailable(target.data())) {
    return { status: "PHOTO_AVAILABLE", requestedAtMillis: 0, retryAfterMillis: 0 };
  }
  if (!request.exists) {
    return { status: "NONE", requestedAtMillis: 0, retryAfterMillis: 0 };
  }

  const value = request.data() || {};
  const requestedAtMillis = value.requestedAt instanceof admin.firestore.Timestamp ?
    value.requestedAt.toMillis() : Number(value.requestedAtMillis || 0);
  const status = String(value.status || "NONE").toUpperCase();
  const retryAfterMillis = status === "PENDING" ?
    Math.max(0, requestedAtMillis + PHOTO_REQUEST_COOLDOWN_MS - Date.now()) : 0;

  return { status, requestedAtMillis, retryAfterMillis };
});

/**
 * Ask an eligible member to add a public approved profile photo.
 *
 * This never grants access to hidden/private media and never changes the target's privacy settings.
 */
export const requestProfilePhoto = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const requesterUid = context.auth?.uid;
  if (!requesterUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = cleanUid(data?.targetUid);
  if (targetUid === requesterUid) {
    throw new functions.https.HttpsError("invalid-argument", "Cannot request your own photo");
  }

  const now = Date.now();
  const reqRef = db.collection("photoRequests").doc(requestId(requesterUid, targetUid));
  const rateRef = db.collection("photoRequestRateLimits").doc(`${requesterUid}_${dayKey(now)}`);
  const requesterRef = db.collection("users").doc(requesterUid);
  const targetRef = db.collection("users").doc(targetUid);
  const requesterBlockRef = db.collection("blocks").doc(requesterUid).collection("blocked").doc(targetUid);
  const targetBlockRef = db.collection("blocks").doc(targetUid).collection("blocked").doc(requesterUid);
  const requesterPrivacyRef = db.collection("privacyRelations").doc(requesterUid).collection("members").doc(targetUid);
  const targetPrivacyRef = db.collection("privacyRelations").doc(targetUid).collection("members").doc(requesterUid);
  const targetInterestRef = db.collection("interests").doc(`${targetUid}_${requesterUid}`);

  const result = await db.runTransaction(async (tx) => {
    const [
      requester,
      target,
      existing,
      rate,
      requesterBlock,
      targetBlock,
      requesterPrivacy,
      targetPrivacy,
      targetInterest,
    ] = await Promise.all([
      tx.get(requesterRef),
      tx.get(targetRef),
      tx.get(reqRef),
      tx.get(rateRef),
      tx.get(requesterBlockRef),
      tx.get(targetBlockRef),
      tx.get(requesterPrivacyRef),
      tx.get(targetPrivacyRef),
      tx.get(targetInterestRef),
    ]);

    if (!requester.exists || !target.exists ||
        !accountIsActive(requester.data()?.accountStatus) ||
        !accountIsActive(target.data()?.accountStatus)) {
      throw new functions.https.HttpsError("failed-precondition", "Photo request is unavailable while an account is inactive");
    }
    if (requesterBlock.exists || targetBlock.exists) {
      throw new functions.https.HttpsError("permission-denied", "Photo request is unavailable after a block");
    }
    if (requesterPrivacy.data()?.profileHidden === true ||
        targetPrivacy.data()?.profileHidden === true) {
      throw new functions.https.HttpsError("permission-denied", "Photo request is unavailable for this privacy relationship");
    }
    if (target.data()?.stealthMode === true && !targetInterest.exists) {
      throw new functions.https.HttpsError("permission-denied", "This profile is not currently available for a photo request");
    }
    if (publicPhotoAvailable(target.data())) {
      return { status: "PHOTO_AVAILABLE", requestedAtMillis: 0, retryAfterMillis: 0 };
    }

    const existingData = existing.data() || {};
    const requestedAtMillis = existingData.requestedAt instanceof admin.firestore.Timestamp ?
      existingData.requestedAt.toMillis() : Number(existingData.requestedAtMillis || 0);
    const existingStatus = String(existingData.status || "").toUpperCase();

    if (existing.exists && existingStatus === "PENDING" &&
        requestedAtMillis > now - PHOTO_REQUEST_COOLDOWN_MS) {
      return {
        status: "PENDING",
        requestedAtMillis,
        retryAfterMillis: Math.max(0, requestedAtMillis + PHOTO_REQUEST_COOLDOWN_MS - now),
      };
    }

    const count = Number(rate.data()?.count || 0);
    if (count >= PHOTO_REQUEST_DAILY_LIMIT) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "Daily photo request limit reached. Try again tomorrow."
      );
    }

    const stamp = admin.firestore.FieldValue.serverTimestamp();
    tx.set(reqRef, {
      requesterUid,
      targetUid,
      status: "PENDING",
      requestedAt: stamp,
      requestedAtMillis: now,
      fulfilledAt: null,
      updatedAt: stamp,
    }, { merge: false });
    tx.set(rateRef, {
      requesterUid,
      day: dayKey(now),
      count: count + 1,
      updatedAt: stamp,
    }, { merge: false });

    return { status: "PENDING", requestedAtMillis: now, retryAfterMillis: PHOTO_REQUEST_COOLDOWN_MS };
  });

  if (result.status === "PENDING" && result.requestedAtMillis === now) {
    const notificationId = `photo_request_${requesterUid}_${targetUid}_${now}`;
    await persistAndSendNotification({
      notificationId,
      userId: targetUid,
      type: "PHOTO_REQUEST",
      title: "Photo request",
      body: "A member viewed your profile and requested that you add a profile photo.",
      entityType: "profile",
      entityId: requesterUid,
      deepLink: notificationDeepLink("notifications", functions.config().app_links?.host),
      action: notificationActionFor("PHOTO_REQUEST"),
      pushType: "photo_request",
      preferenceKey: "system",
      fromFirebaseUid: requesterUid,
    });
  }

  return result;
});

/**
 * Mark outstanding requests fulfilled when an approved public primary photo becomes available.
 */
export const onPublicProfilePhotoAvailable = functions.firestore
  .document("users/{uid}")
  .onUpdate(async (change, context) => {
    if (publicPhotoAvailable(change.before.data()) || !publicPhotoAvailable(change.after.data())) return;
    const targetUid = context.params.uid;
    const pending = await db.collection("photoRequests")
      .where("targetUid", "==", targetUid)
      .limit(250)
      .get();
    if (pending.empty) return;

    const batch = db.batch();
    const stamp = admin.firestore.FieldValue.serverTimestamp();
    pending.docs
      .filter((doc) => String(doc.data()?.status || "").toUpperCase() === "PENDING")
      .forEach((doc) => {
      batch.update(doc.ref, {
        status: "FULFILLED",
        fulfilledAt: stamp,
        updatedAt: stamp,
      });
    });
    await batch.commit();
  });

/** Remove photo-request state tied to a deleted account. */
export const cleanupPhotoRequestsOnUserDelete = functions.firestore
  .document("users/{uid}")
  .onDelete(async (_snap, context) => {
    const uid = context.params.uid;
    const [sent, received] = await Promise.all([
      db.collection("photoRequests").where("requesterUid", "==", uid).limit(250).get(),
      db.collection("photoRequests").where("targetUid", "==", uid).limit(250).get(),
    ]);
    const refs = new Map<string, FirebaseFirestore.DocumentReference>();
    [...sent.docs, ...received.docs].forEach((doc) => refs.set(doc.ref.path, doc.ref));
    if (refs.size === 0) return;
    const batch = db.batch();
    refs.forEach((ref) => batch.delete(ref));
    await batch.commit();
  });
