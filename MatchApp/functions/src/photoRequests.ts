import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import * as crypto from "crypto";
import {
  db,
  requireAppCheck,
} from "./shared";
import { accountIsActive } from "./accountStatusPolicy";
import {
  PHOTO_REQUEST_COOLDOWN_MS,
  photoRequestCooldownRemaining,
  photoRequestDailyAllowed,
  publicPhotoAvailable,
} from "./photoRequestPolicy";

function cleanUid(value: unknown): string {
  const uid = typeof value === "string" ? value.trim() : "";
  if (!uid || uid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "A valid target member is required");
  }
  return uid;
}

function stableId(...parts: string[]): string {
  return crypto.createHash("sha256").update(parts.join("\u0000"), "utf8").digest("hex");
}

function requestId(requesterUid: string, targetUid: string): string {
  return stableId("photo-request", requesterUid, targetUid);
}

function rateId(requesterUid: string, day: string): string {
  return stableId("photo-request-rate", requesterUid, day);
}

function dayKey(now: number): string {
  return new Date(now).toISOString().slice(0, 10);
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

  const [
    requester,
    target,
    request,
    requesterBlock,
    targetBlock,
    requesterPrivacy,
    targetPrivacy,
    targetInterest,
  ] = await Promise.all([
    db.collection("users").doc(requesterUid).get(),
    db.collection("users").doc(targetUid).get(),
    db.collection("photoRequests").doc(requestId(requesterUid, targetUid)).get(),
    db.collection("blocks").doc(requesterUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(requesterUid).get(),
    db.collection("privacyRelations").doc(requesterUid).collection("members").doc(targetUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(requesterUid).get(),
    db.collection("interests").doc(`${targetUid}_${requesterUid}`).get(),
  ]);

  if (!requester.exists || !target.exists ||
      !accountIsActive(requester.data()?.accountStatus) ||
      !accountIsActive(target.data()?.accountStatus) ||
      requesterBlock.exists || targetBlock.exists ||
      requesterPrivacy.data()?.profileHidden === true ||
      targetPrivacy.data()?.profileHidden === true ||
      (target.data()?.stealthMode === true && !targetInterest.exists)) {
    return { status: "UNAVAILABLE", requestedAtMillis: 0, retryAfterMillis: 0 };
  }
  if (publicPhotoAvailable(target.data()?.photoUrl)) {
    return { status: "PHOTO_AVAILABLE", requestedAtMillis: 0, retryAfterMillis: 0 };
  }
  if (!request.exists) {
    return { status: "NONE", requestedAtMillis: 0, retryAfterMillis: 0 };
  }

  const value = request.data() || {};
  const requestedAtMillis = value.requestedAt instanceof admin.firestore.Timestamp ?
    value.requestedAt.toMillis() : Number(value.requestedAtMillis || 0);
  const status = String(value.status || "NONE").toUpperCase();
  const retryAfterMillis = photoRequestCooldownRemaining(
    status,
    requestedAtMillis,
    Date.now()
  );

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
  const rateRef = db.collection("photoRequestRateLimits").doc(rateId(requesterUid, dayKey(now)));
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
    if (publicPhotoAvailable(target.data()?.photoUrl)) {
      return { status: "PHOTO_AVAILABLE", requestedAtMillis: 0, retryAfterMillis: 0 };
    }

    const existingData = existing.data() || {};
    const requestedAtMillis = existingData.requestedAt instanceof admin.firestore.Timestamp ?
      existingData.requestedAt.toMillis() : Number(existingData.requestedAtMillis || 0);
    const existingStatus = String(existingData.status || "").toUpperCase();

    const retryAfterMillis = photoRequestCooldownRemaining(
      existingStatus,
      requestedAtMillis,
      now
    );
    if (existing.exists && retryAfterMillis > 0) {
      return {
        status: "PENDING",
        requestedAtMillis,
        retryAfterMillis,
      };
    }

    const count = Number(rate.data()?.count || 0);
    if (!photoRequestDailyAllowed(count)) {
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



  return result;
});

/**
 * Mark outstanding requests fulfilled when an approved public primary photo becomes available.
 */
export const onPublicProfilePhotoAvailable = functions.firestore
  .document("users/{uid}")
  .onUpdate(async (change, context) => {
    if (publicPhotoAvailable(change.before.data()?.photoUrl) || !publicPhotoAvailable(change.after.data()?.photoUrl)) return;
    const targetUid = context.params.uid;
    const pending = await db.collection("photoRequests")
      .where("targetUid", "==", targetUid)
      .get();
    const docs = pending.docs
      .filter((doc) => String(doc.data()?.status || "").toUpperCase() === "PENDING");
    if (docs.length === 0) return;

    const stamp = admin.firestore.FieldValue.serverTimestamp();
    for (let offset = 0; offset < docs.length; offset += 450) {
      const batch = db.batch();
      docs.slice(offset, offset + 450).forEach((doc) => {
        batch.update(doc.ref, {
          status: "FULFILLED",
          fulfilledAt: stamp,
          updatedAt: stamp,
        });
      });
      await batch.commit();
    }
  });

/** Remove photo-request state tied to a deleted account. */
export const cleanupPhotoRequestsOnUserDelete = functions.firestore
  .document("users/{uid}")
  .onDelete(async (_snap, context) => {
    const uid = context.params.uid;
    const [sent, received, rates] = await Promise.all([
      db.collection("photoRequests").where("requesterUid", "==", uid).get(),
      db.collection("photoRequests").where("targetUid", "==", uid).get(),
      db.collection("photoRequestRateLimits").where("requesterUid", "==", uid).get(),
    ]);
    const refs = new Map<string, FirebaseFirestore.DocumentReference>();
    [...sent.docs, ...received.docs, ...rates.docs].forEach((doc) => refs.set(doc.ref.path, doc.ref));
    const all = [...refs.values()];
    for (let offset = 0; offset < all.length; offset += 450) {
      const batch = db.batch();
      all.slice(offset, offset + 450).forEach((ref) => batch.delete(ref));
      await batch.commit();
    }
  });
