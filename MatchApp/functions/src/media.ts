import * as admin from "firebase-admin";
import * as crypto from "crypto";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck, requireOpsRole } from "./shared";
import { requireActiveConsent } from "./consent";

const MAX_PROFILE_PHOTO_BYTES = 2 * 1024 * 1024;
const MAX_PROFILE_VIDEO_BYTES = 50 * 1024 * 1024;

function moderationIdForPath(path: string): string {
  return crypto.createHash("sha256").update(path).digest("hex");
}

function ownedMediaPath(
  value: unknown,
  uid: string,
  folder: "photos" | "videos"
): string {
  const path = typeof value === "string" ? value.trim() : "";
  const prefix = `${folder}/${uid}/`;
  if (!path.startsWith(prefix) || path.length > 512 || path.includes("..")) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `Invalid profile ${folder === "photos" ? "photo" : "video"} path`
    );
  }
  return path;
}

function profilePhotoPath(value: unknown, uid: string): string {
  return ownedMediaPath(value, uid, "photos");
}

function profileVideoPath(value: unknown, uid: string): string {
  return ownedMediaPath(value, uid, "videos");
}

function reviewDecision(value: unknown): "APPROVED" | "REJECTED" {
  const decision = typeof value === "string" ? value.trim().toUpperCase() : "";
  if (decision !== "APPROVED" && decision !== "REJECTED") {
    throw new functions.https.HttpsError("invalid-argument", "Invalid moderation decision");
  }
  return decision;
}

function reviewReason(value: unknown): string {
  const reason = typeof value === "string" ? value.trim() : "";
  if (reason.length < 3 || reason.length > 500) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "A 3-500 character moderation reason is required"
    );
  }
  return reason;
}

/**
 * Only Admin SDK code can add this metadata because Storage rules restrict member uploads to the
 * ownerUid metadata key. Peers may read photos/videos only when published=true.
 */
async function setPublishedMetadata(storagePath: string, published: boolean): Promise<void> {
  const file = admin.storage().bucket().file(storagePath);
  const [metadata] = await file.getMetadata();
  await file.setMetadata({
    metadata: {
      ...(metadata.metadata || {}),
      published: published ? "true" : "false",
    },
  });
}

async function switchPublishedPointer(
  uid: string,
  field: "photoUrl" | "videoUrl",
  newPath: string
): Promise<void> {
  const userRef = db.collection("users").doc(uid);
  const user = await userRef.get();
  if (!user.exists) {
    throw new functions.https.HttpsError("failed-precondition", "Profile is unavailable");
  }
  const oldPath = String(user.data()?.[field] || "");
  if (oldPath === newPath) {
    if (newPath) await setPublishedMetadata(newPath, true);
    return;
  }

  if (oldPath) {
    await setPublishedMetadata(oldPath, false);
  }
  try {
    if (newPath) await setPublishedMetadata(newPath, true);
    await userRef.set({
      [field]: newPath,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  } catch (error) {
    if (newPath) {
      await setPublishedMetadata(newPath, false).catch(() => undefined);
    }
    if (oldPath) {
      await setPublishedMetadata(oldPath, true).catch(() => undefined);
    }
    throw error;
  }
}

/**
 * Registers an owner-uploaded object for moderation. The callable never trusts an upload as public.
 * Peers can read it only after an authorized moderator approves it and trusted backend publication
 * sets both users.photoUrl and protected Storage metadata.
 */
export const submitProfilePhoto = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  await requireActiveConsent(uid, "media_processing");

  const storagePath = profilePhotoPath(data?.storagePath, uid);
  const file = admin.storage().bucket().file(storagePath);
  const [exists] = await file.exists();
  if (!exists) {
    throw new functions.https.HttpsError("failed-precondition", "Uploaded photo was not found");
  }

  const [metadata] = await file.getMetadata();
  const ownerUid = metadata.metadata?.ownerUid;
  const contentType = String(metadata.contentType || "");
  const size = Number(metadata.size || 0);
  if (ownerUid !== uid || !contentType.startsWith("image/") ||
      !Number.isFinite(size) || size <= 0 || size > MAX_PROFILE_PHOTO_BYTES) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Uploaded photo does not satisfy the protected media contract"
    );
  }

  const moderationId = moderationIdForPath(storagePath);
  const moderationRef = db.collection("photoModeration").doc(moderationId);
  const rawFingerprint = typeof metadata.md5Hash === "string" ? metadata.md5Hash : "";
  const fingerprintId = rawFingerprint
    ? crypto.createHash("sha256").update(rawFingerprint).digest("hex")
    : "";
  const fingerprintRef = fingerprintId
    ? db.collection("photoFingerprints").doc(fingerprintId)
    : null;

  await db.runTransaction(async (tx) => {
    const [existing, fingerprint] = await Promise.all([
      tx.get(moderationRef),
      fingerprintRef ? tx.get(fingerprintRef) : Promise.resolve(null),
    ]);
    if (existing.exists) {
      const value = existing.data() || {};
      if (value.uid !== uid || value.storagePath !== storagePath) {
        throw new functions.https.HttpsError("permission-denied", "Photo ownership mismatch");
      }
      return;
    }

    const owners = fingerprint?.exists && Array.isArray(fingerprint.data()?.owners)
      ? fingerprint.data()?.owners.filter((item: unknown): item is string =>
        typeof item === "string" && item.length <= 128)
      : [];
    const duplicateAcrossAccounts = owners.some((owner: string) => owner !== uid);

    tx.create(moderationRef, {
      uid,
      storagePath,
      contentType,
      size,
      duplicateAcrossAccounts,
      status: "PENDING",
      publicationStatus: "NOT_PUBLISHED",
      source: "ANDROID",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    if (fingerprintRef && !owners.includes(uid)) {
      tx.set(fingerprintRef, {
        owners: [...owners.slice(0, 49), uid],
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }
    if (duplicateAcrossAccounts) {
      tx.set(db.collection("riskSignals").doc(uid), {
        duplicatePhotoSignalCount: admin.firestore.FieldValue.increment(1),
        lastDuplicatePhotoSignalAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }
  });

  return { moderationId, status: "PENDING", storagePath };
});

/** Authorized moderation queue. Raw image bytes remain in protected Storage. */
export const listPendingPhotoModeration = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["moderator", "ops_admin"]);
  const parsed = Number(data?.limit ?? 50);
  const limit = Number.isFinite(parsed) ? Math.max(1, Math.min(100, Math.floor(parsed))) : 50;
  const snapshot = await db.collection("photoModeration")
    .where("status", "==", "PENDING")
    .limit(limit)
    .get();

  return {
    items: snapshot.docs.map((doc) => {
      const value = doc.data();
      return {
        id: doc.id,
        uid: String(value.uid || ""),
        storagePath: String(value.storagePath || ""),
        contentType: String(value.contentType || ""),
        size: Number(value.size || 0),
        duplicateAcrossAccounts: value.duplicateAcrossAccounts === true,
        createdAtMillis: value.createdAt instanceof admin.firestore.Timestamp
          ? value.createdAt.toMillis()
          : null,
      };
    }),
  };
});

/**
 * Human review makes an uploaded photo eligible for publication. The first approved photo is
 * published automatically only after trusted Storage publication metadata succeeds.
 */
export const reviewProfilePhoto = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["moderator", "ops_admin"]);
  const moderationId = typeof data?.moderationId === "string"
    ? data.moderationId.trim()
    : "";
  if (!moderationId.match(/^[a-f0-9]{64}$/)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid photo moderation case");
  }
  const decision = reviewDecision(data?.decision);
  const reason = reviewReason(data?.reason);

  const moderationRef = db.collection("photoModeration").doc(moderationId);
  const auditRef = db.collection("opsAuditLog").doc();
  let storagePath = "";
  let ownerUid = "";

  await db.runTransaction(async (tx) => {
    const moderation = await tx.get(moderationRef);
    if (!moderation.exists) {
      throw new functions.https.HttpsError("not-found", "Photo moderation case not found");
    }
    const current = moderation.data() || {};
    ownerUid = String(current.uid || "");
    storagePath = String(current.storagePath || "");
    if (!ownerUid || !storagePath) {
      throw new functions.https.HttpsError("failed-precondition", "Photo case is malformed");
    }
    if (String(current.status || "") !== "PENDING") {
      throw new functions.https.HttpsError("failed-precondition", "Photo was already reviewed");
    }

    tx.update(moderationRef, {
      status: decision,
      reviewReason: reason,
      reviewedBy: actor.uid,
      reviewedRole: actor.role,
      reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    tx.create(auditRef, {
      actorUid: actor.uid,
      actorRole: actor.role,
      action: "PROFILE_PHOTO_REVIEWED",
      targetCollection: "photoModeration",
      targetId: moderationId,
      before: { status: "PENDING" },
      after: { status: decision },
      reason,
      ownerUid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  if (decision === "REJECTED") {
    await admin.storage().bucket().file(storagePath).delete({ ignoreNotFound: true });
    return { success: true, moderationId, status: decision, publicationStatus: "REJECTED" };
  }

  const userRef = db.collection("users").doc(ownerUid);
  const user = await userRef.get();
  if (user.exists && !String(user.data()?.photoUrl || "").trim()) {
    try {
      await switchPublishedPointer(ownerUid, "photoUrl", storagePath);
      await moderationRef.set({
        publicationStatus: "PUBLISHED",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
      return { success: true, moderationId, status: decision, publicationStatus: "PUBLISHED" };
    } catch (error) {
      await moderationRef.set({
        publicationStatus: "PUBLISH_FAILED",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
      throw error;
    }
  }

  await moderationRef.set({
    publicationStatus: "APPROVED_NOT_PRIMARY",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  return { success: true, moderationId, status: decision, publicationStatus: "APPROVED_NOT_PRIMARY" };
});

/** Only an approved object may become the public primary profile photo. */
export const setPrimaryApprovedPhoto = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const storagePath = profilePhotoPath(data?.storagePath, uid);
  const moderationRef = db.collection("photoModeration").doc(moderationIdForPath(storagePath));
  const moderation = await moderationRef.get();
  const value = moderation.data() || {};
  if (!moderation.exists || value.uid !== uid || value.storagePath !== storagePath ||
      value.status !== "APPROVED") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "This photo has not been approved for profile publication"
    );
  }

  await switchPublishedPointer(uid, "photoUrl", storagePath);
  await moderationRef.set({
    publicationStatus: "PUBLISHED",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  return { success: true, storagePath };
});

/**
 * If a profile-media object is deleted, remove stale moderation authority and public pointers.
 */
export const onProfileMediaDeleted = functions.storage.object().onDelete(async (object) => {
  const storagePath = object.name || "";
  const photoMatch = storagePath.match(/^photos\/([^/]+)\//);
  const videoMatch = storagePath.match(/^videos\/([^/]+)\//);
  if (!photoMatch && !videoMatch) return;

  const uid = (photoMatch || videoMatch)?.[1] || "";
  const isPhoto = Boolean(photoMatch);
  const moderationCollection = isPhoto ? "photoModeration" : "videoModeration";
  const publicField = isPhoto ? "photoUrl" : "videoUrl";
  const moderationRef = db.collection(moderationCollection).doc(moderationIdForPath(storagePath));
  const userRef = db.collection("users").doc(uid);

  await db.runTransaction(async (tx) => {
    const [moderation, user] = await Promise.all([
      tx.get(moderationRef),
      tx.get(userRef),
    ]);
    if (moderation.exists) tx.delete(moderationRef);
    if (user.exists && user.data()?.[publicField] === storagePath) {
      tx.update(userRef, { [publicField]: "" });
    }
  });

  if (!isPhoto) {
    const stateRef = db.collection("profileVideoState").doc(uid);
    const state = await stateRef.get();
    if (state.exists && (
      state.data()?.storagePath === storagePath ||
      state.data()?.publishedPath === storagePath
    )) {
      await stateRef.set({
        status: "REMOVED",
        storagePath: "",
        publishedPath: "",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }
  }
});

/**
 * Returns a five-minute signed URL for a pending profile-photo review case. Member Storage rules
 * stay private for pending objects; access is role-gated and audited here.
 */
export const getPhotoModerationReviewCase = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["moderator", "ops_admin"]);
  const moderationId = typeof data?.moderationId === "string"
    ? data.moderationId.trim()
    : "";
  if (!moderationId.match(/^[a-f0-9]{64}$/)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid photo moderation case");
  }

  const review = await db.collection("photoModeration").doc(moderationId).get();
  if (!review.exists || String(review.data()?.status || "") !== "PENDING") {
    throw new functions.https.HttpsError("not-found", "Pending photo moderation case not found");
  }
  const value = review.data() || {};
  const ownerUid = String(value.uid || "");
  const storagePath = String(value.storagePath || "");
  if (!ownerUid || !storagePath.startsWith(`photos/${ownerUid}/`) || storagePath.includes("..")) {
    throw new functions.https.HttpsError("failed-precondition", "Photo moderation case is malformed");
  }

  const expiresAtMillis = Date.now() + 5 * 60 * 1000;
  const [documentUrl] = await admin.storage().bucket().file(storagePath).getSignedUrl({
    action: "read",
    expires: expiresAtMillis,
  });
  await db.collection("opsAuditLog").add({
    actorUid: actor.uid,
    actorRole: actor.role,
    action: "PROFILE_PHOTO_ACCESSED",
    targetCollection: "photoModeration",
    targetId: moderationId,
    ownerUid,
    reason: "PHOTO_REVIEW",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { moderationId, uid: ownerUid, storagePath, documentUrl, expiresAtMillis };
});

/**
 * Video profiles use the same authority model as photos: the owner may upload a protected object,
 * but only the backend can make it peer-readable and write users.videoUrl.
 */
export const submitProfileVideo = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  await requireActiveConsent(uid, "media_processing");

  const storagePath = profileVideoPath(data?.storagePath, uid);
  const file = admin.storage().bucket().file(storagePath);
  const [exists] = await file.exists();
  if (!exists) {
    throw new functions.https.HttpsError("failed-precondition", "Uploaded video was not found");
  }
  const [metadata] = await file.getMetadata();
  const ownerUid = metadata.metadata?.ownerUid;
  const contentType = String(metadata.contentType || "");
  const size = Number(metadata.size || 0);
  if (ownerUid !== uid || !contentType.startsWith("video/") ||
      !Number.isFinite(size) || size <= 0 || size > MAX_PROFILE_VIDEO_BYTES) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Uploaded video does not satisfy the protected media contract"
    );
  }

  const stateRef = db.collection("profileVideoState").doc(uid);
  const existingState = await stateRef.get();
  if (String(existingState.data()?.status || "") === "PENDING") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "A profile video is already pending review"
    );
  }

  const moderationId = moderationIdForPath(storagePath);
  const moderationRef = db.collection("videoModeration").doc(moderationId);
  await db.runTransaction(async (tx) => {
    const existing = await tx.get(moderationRef);
    if (existing.exists) {
      const value = existing.data() || {};
      if (value.uid !== uid || value.storagePath !== storagePath) {
        throw new functions.https.HttpsError("permission-denied", "Video ownership mismatch");
      }
      return;
    }
    tx.create(moderationRef, {
      uid,
      storagePath,
      contentType,
      size,
      status: "PENDING",
      publicationStatus: "NOT_PUBLISHED",
      source: "ANDROID",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.set(stateRef, {
      uid,
      storagePath,
      status: "PENDING",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  return { moderationId, status: "PENDING", storagePath };
});

export const getMyProfileVideoState = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const [user, state] = await Promise.all([
    db.collection("users").doc(uid).get(),
    db.collection("profileVideoState").doc(uid).get(),
  ]);
  return {
    status: String(state.data()?.status || (user.data()?.videoUrl ? "PUBLISHED" : "NONE")),
    pendingPath: String(state.data()?.status || "") === "PENDING"
      ? String(state.data()?.storagePath || "")
      : "",
    publishedPath: String(user.data()?.videoUrl || ""),
  };
});

export const listPendingVideoModeration = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["moderator", "ops_admin"]);
  const parsed = Number(data?.limit ?? 50);
  const limit = Number.isFinite(parsed) ? Math.max(1, Math.min(100, Math.floor(parsed))) : 50;
  const snapshot = await db.collection("videoModeration")
    .where("status", "==", "PENDING")
    .limit(limit)
    .get();
  return {
    items: snapshot.docs.map((doc) => {
      const value = doc.data();
      return {
        id: doc.id,
        uid: String(value.uid || ""),
        storagePath: String(value.storagePath || ""),
        contentType: String(value.contentType || ""),
        size: Number(value.size || 0),
        createdAtMillis: value.createdAt instanceof admin.firestore.Timestamp
          ? value.createdAt.toMillis()
          : null,
      };
    }),
  };
});

export const getVideoModerationReviewCase = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["moderator", "ops_admin"]);
  const moderationId = typeof data?.moderationId === "string"
    ? data.moderationId.trim()
    : "";
  if (!moderationId.match(/^[a-f0-9]{64}$/)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid video moderation case");
  }
  const review = await db.collection("videoModeration").doc(moderationId).get();
  if (!review.exists || String(review.data()?.status || "") !== "PENDING") {
    throw new functions.https.HttpsError("not-found", "Pending video moderation case not found");
  }
  const value = review.data() || {};
  const ownerUid = String(value.uid || "");
  const storagePath = String(value.storagePath || "");
  if (!ownerUid || !storagePath.startsWith(`videos/${ownerUid}/`) || storagePath.includes("..")) {
    throw new functions.https.HttpsError("failed-precondition", "Video moderation case is malformed");
  }

  const expiresAtMillis = Date.now() + 5 * 60 * 1000;
  const [documentUrl] = await admin.storage().bucket().file(storagePath).getSignedUrl({
    action: "read",
    expires: expiresAtMillis,
  });
  await db.collection("opsAuditLog").add({
    actorUid: actor.uid,
    actorRole: actor.role,
    action: "PROFILE_VIDEO_ACCESSED",
    targetCollection: "videoModeration",
    targetId: moderationId,
    ownerUid,
    reason: "VIDEO_REVIEW",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { moderationId, uid: ownerUid, storagePath, documentUrl, expiresAtMillis };
});

export const reviewProfileVideo = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["moderator", "ops_admin"]);
  const moderationId = typeof data?.moderationId === "string"
    ? data.moderationId.trim()
    : "";
  if (!moderationId.match(/^[a-f0-9]{64}$/)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid video moderation case");
  }
  const decision = reviewDecision(data?.decision);
  const reason = reviewReason(data?.reason);
  const moderationRef = db.collection("videoModeration").doc(moderationId);
  const moderation = await moderationRef.get();
  if (!moderation.exists || String(moderation.data()?.status || "") !== "PENDING") {
    throw new functions.https.HttpsError("failed-precondition", "Video is not pending review");
  }
  const ownerUid = String(moderation.data()?.uid || "");
  const storagePath = String(moderation.data()?.storagePath || "");
  if (!ownerUid || !storagePath.startsWith(`videos/${ownerUid}/`)) {
    throw new functions.https.HttpsError("failed-precondition", "Video case is malformed");
  }

  const auditRef = db.collection("opsAuditLog").doc();
  await db.runTransaction(async (tx) => {
    const fresh = await tx.get(moderationRef);
    if (!fresh.exists || String(fresh.data()?.status || "") !== "PENDING") {
      throw new functions.https.HttpsError("failed-precondition", "Video was already reviewed");
    }
    tx.update(moderationRef, {
      status: decision,
      reviewReason: reason,
      reviewedBy: actor.uid,
      reviewedRole: actor.role,
      reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.create(auditRef, {
      actorUid: actor.uid,
      actorRole: actor.role,
      action: "PROFILE_VIDEO_REVIEWED",
      targetCollection: "videoModeration",
      targetId: moderationId,
      before: { status: "PENDING" },
      after: { status: decision },
      reason,
      ownerUid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  if (decision === "REJECTED") {
    await admin.storage().bucket().file(storagePath).delete({ ignoreNotFound: true });
    await db.collection("profileVideoState").doc(ownerUid).set({
      status: "REJECTED",
      storagePath: "",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    return { success: true, moderationId, status: decision };
  }

  await switchPublishedPointer(ownerUid, "videoUrl", storagePath);
  await Promise.all([
    moderationRef.set({
      publicationStatus: "PUBLISHED",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true }),
    db.collection("profileVideoState").doc(ownerUid).set({
      status: "PUBLISHED",
      storagePath: "",
      publishedPath: storagePath,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true }),
  ]);
  return { success: true, moderationId, status: decision, publicationStatus: "PUBLISHED" };
});

export const removeProfileVideo = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const userRef = db.collection("users").doc(uid);
  const stateRef = db.collection("profileVideoState").doc(uid);
  const [user, state] = await Promise.all([userRef.get(), stateRef.get()]);
  const publishedPath = String(user.data()?.videoUrl || "");
  const pendingPath = String(state.data()?.status || "") === "PENDING"
    ? String(state.data()?.storagePath || "")
    : "";

  if (publishedPath) {
    await switchPublishedPointer(uid, "videoUrl", "");
    await admin.storage().bucket().file(publishedPath).delete({ ignoreNotFound: true });
  }
  if (pendingPath) {
    await admin.storage().bucket().file(pendingPath).delete({ ignoreNotFound: true });
  }
  await stateRef.set({
    status: "REMOVED",
    storagePath: "",
    publishedPath: "",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  return { success: true };
});
